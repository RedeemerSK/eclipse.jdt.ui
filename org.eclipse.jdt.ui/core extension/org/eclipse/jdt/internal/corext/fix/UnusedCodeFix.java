/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.JavadocTagsSubProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix extends AbstractFix {
	
	private static class SideEffectFinder extends ASTVisitor {

		private ArrayList fSideEffectNodes;

		public SideEffectFinder(ArrayList res) {
			fSideEffectNodes= res;
		}

		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(PrefixExpression node) {
			Object operator= node.getOperator();
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				fSideEffectNodes.add(node);
			}
			return false;
		}

		public boolean visit(MethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}
	}
	
	private static class RemoveImportOperation implements IFixRewriteOperation {

		private final ImportDeclaration fImportDeclaration;
		
		public RemoveImportOperation(ImportDeclaration importDeclaration) {
			fImportDeclaration= importDeclaration;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			ImportDeclaration node= fImportDeclaration;
			TextEditGroup group= new TextEditGroup(FixMessages.UnusedCodeFix_RemoveImport_description + " " + node.getName()); //$NON-NLS-1$
			cuRewrite.getASTRewrite().remove(node, group);
			textEditGroups.add(group);
		}
		
	}
	
	private static class RemoveUnusedMemberOperation implements IFixRewriteOperation {

		private final SimpleName[] fUnusedNames;
		
		public RemoveUnusedMemberOperation(SimpleName[] unusedNames) {
			fUnusedNames= unusedNames;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.fix.AbstractFix.IFixRewriteOperation#rewriteAST(org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, java.util.List)
		 */
		public void rewriteAST(CompilationUnitRewrite cuRewrite, List textEditGroups) throws CoreException {
			for (int i= 0; i < fUnusedNames.length; i++) {
				removeUnusedName(cuRewrite.getASTRewrite(), fUnusedNames[i], cuRewrite.getRoot(), textEditGroups);	
			}
		}
		
		private void removeUnusedName(ASTRewrite rewrite, SimpleName simpleName, CompilationUnit completeRoot, List groups) {
			IBinding binding= simpleName.resolveBinding();
			CompilationUnit root= (CompilationUnit) simpleName.getRoot();
			String displayString= getDisplayString(simpleName, binding);
			TextEditGroup group= new TextEditGroup(displayString);
			groups.add(group);
			if (binding.getKind() == IBinding.METHOD) {
				IMethodBinding decl= ((IMethodBinding) binding).getMethodDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else if (binding.getKind() == IBinding.TYPE) {
				ITypeBinding decl= ((ITypeBinding) binding).getTypeDeclaration();
				ASTNode declaration= root.findDeclaringNode(decl);
				rewrite.remove(declaration, group);
			} else { // variable
				SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
				SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
				for (int i= 0; i < references.length; i++) {
					removeVariableReferences(rewrite, references[i], group);
				}

				IVariableBinding bindingDecl= ((IVariableBinding) nameNode.resolveBinding()).getVariableDeclaration();
				ASTNode declaringNode= completeRoot.findDeclaringNode(bindingDecl);
				if (declaringNode instanceof SingleVariableDeclaration) {
					removeParamTag(rewrite, (SingleVariableDeclaration) declaringNode, group);
				}
			}
		}
		
		private void removeParamTag(ASTRewrite rewrite, SingleVariableDeclaration varDecl, TextEditGroup group) {
			if (varDecl.getParent() instanceof MethodDeclaration) {
				Javadoc javadoc= ((MethodDeclaration) varDecl.getParent()).getJavadoc();
				if (javadoc != null) {
					TagElement tagElement= JavadocTagsSubProcessor.findParamTag(javadoc, varDecl.getName().getIdentifier());
					if (tagElement != null) {
						rewrite.remove(tagElement, group);
					}
				}
			}
		}
		
		/**
		 * Remove the field or variable declaration including the initializer.
		 */
		private void removeVariableReferences(ASTRewrite rewrite, SimpleName reference, TextEditGroup group) {
			ASTNode parent= reference.getParent();
			while (parent instanceof QualifiedName) {
				parent= parent.getParent();
			}
			if (parent instanceof FieldAccess) {
				parent= parent.getParent();
			}

			int nameParentType= parent.getNodeType();
			if (nameParentType == ASTNode.ASSIGNMENT) {
				Assignment assignment= (Assignment) parent;
				Expression rightHand= assignment.getRightHandSide();

				ASTNode assignParent= assignment.getParent();
				if (assignParent.getNodeType() == ASTNode.EXPRESSION_STATEMENT && rightHand.getNodeType() != ASTNode.ASSIGNMENT) {
					removeVariableWithInitializer(rewrite, rightHand, assignParent, group);
				}	else {
					rewrite.replace(assignment, rewrite.createCopyTarget(rightHand), group);
				}
			} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
				rewrite.remove(parent, group);
			} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				VariableDeclarationFragment frag= (VariableDeclarationFragment) parent;
				ASTNode varDecl= frag.getParent();
				List fragments;
				if (varDecl instanceof VariableDeclarationExpression) {
					fragments= ((VariableDeclarationExpression) varDecl).fragments();
				} else if (varDecl instanceof FieldDeclaration) {
					fragments= ((FieldDeclaration) varDecl).fragments();
				} else {
					fragments= ((VariableDeclarationStatement) varDecl).fragments();
				}
				if (fragments.size() == fUnusedNames.length) {
					rewrite.remove(varDecl, group);
				} else {
					rewrite.remove(frag, group); // don't try to preserve
				}
			}
		}

		private void removeVariableWithInitializer(ASTRewrite rewrite, ASTNode initializerNode, ASTNode statementNode, TextEditGroup group) {
			ArrayList sideEffectNodes= new ArrayList();
			initializerNode.accept(new SideEffectFinder(sideEffectNodes));
			int nSideEffects= sideEffectNodes.size();
			if (nSideEffects == 0) {
				if (ASTNodes.isControlStatementBody(statementNode.getLocationInParent())) {
					rewrite.replace(statementNode, rewrite.getAST().newBlock(), group);
				} else {
					rewrite.remove(statementNode, group);
				}
			} else {
				// do nothing yet
			}
		}
	}
	
	public static UnusedCodeFix createRemoveUnusedImportFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.UnusedImport || id == IProblem.DuplicateImport || id == IProblem.ConflictingImport ||
		    id == IProblem.CannotImportPackage || id == IProblem.ImportNotFound) {
			
			ImportDeclaration node= getImportDeclaration(problem, compilationUnit);
			if (node != null) {
				String label= FixMessages.UnusedCodeFix_RemoveImport_description;
				RemoveImportOperation operation= new RemoveImportOperation(node);
				return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, UnusedCodeCleanUp.REMOVE_UNUSED_IMPORTS);
			}
		}
		return null;
	}
	
	public static UnusedCodeFix createUnusedMemberFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		int id= problem.getProblemId();
		if (id == IProblem.UnusedPrivateMethod || id == IProblem.UnusedPrivateConstructor || id == IProblem.UnusedPrivateField ||
		    id == IProblem.UnusedPrivateType || id == IProblem.LocalVariableIsNeverUsed || id == IProblem.ArgumentIsNeverUsed) {
			
			SimpleName name= getUnusedName(compilationUnit, problem);
			if (name != null) {
				IBinding binding= name.resolveBinding();
				if (binding != null) {
					if (isFormalParameterInEnhancedForStatement(name))
						return null;
						
					String label= getDisplayString(name, binding);
					RemoveUnusedMemberOperation operation= new RemoveUnusedMemberOperation(new SimpleName[] {name});
					return new UnusedCodeFix(label, compilationUnit, new IFixRewriteOperation[] {operation}, getCleanUpFlag(binding));
				}
			}
		}
		return null;
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports) {

		IProblem[] problems= compilationUnit.getProblems();
		IProblemLocation[] locations= new IProblemLocation[problems.length];
		for (int i= 0; i < problems.length; i++) {
			locations[i]= new ProblemLocation(problems[i]);
		}
		
		return createCleanUp(compilationUnit, locations, 
				removeUnusedPrivateMethods, 
				removeUnusedPrivateConstructors, 
				removeUnusedPrivateFields, 
				removeUnusedPrivateTypes, 
				removeUnusedLocalVariables, 
				removeUnusedImports);
	}
	
	public static IFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems, 
			boolean removeUnusedPrivateMethods, 
			boolean removeUnusedPrivateConstructors, 
			boolean removeUnusedPrivateFields, 
			boolean removeUnusedPrivateTypes, 
			boolean removeUnusedLocalVariables, 
			boolean removeUnusedImports) {

		List/*<IFixRewriteOperation>*/ result= new ArrayList();
		Hashtable/*<ASTNode, List>*/ variableDeclarations= new Hashtable();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation problem= problems[i];
			int id= problem.getProblemId();
			
			if (removeUnusedImports && id == IProblem.UnusedImport) {
				ImportDeclaration node= UnusedCodeFix.getImportDeclaration(problem, compilationUnit);
				if (node != null) {
					result.add(new RemoveImportOperation(node));
				}
			}

			if ((removeUnusedPrivateMethods && id == IProblem.UnusedPrivateMethod) || (removeUnusedPrivateConstructors && id == IProblem.UnusedPrivateConstructor) ||
			    (removeUnusedPrivateTypes && id == IProblem.UnusedPrivateType)) {
				
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null) {
						result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}));
					}
				}
			}
			
			if ((removeUnusedLocalVariables && id == IProblem.LocalVariableIsNeverUsed) ||  (removeUnusedPrivateFields && id == IProblem.UnusedPrivateField)) {
				SimpleName name= getUnusedName(compilationUnit, problem);
				if (name != null) {
					IBinding binding= name.resolveBinding();
					if (binding != null && !isFormalParameterInEnhancedForStatement(name) && isSideEffectFree(name, compilationUnit)) {
						VariableDeclarationFragment parent= (VariableDeclarationFragment)ASTNodes.getParent(name, VariableDeclarationFragment.class);
						if (parent != null) {
							ASTNode varDecl= parent.getParent();
							if (!variableDeclarations.containsKey(varDecl)) {
								variableDeclarations.put(varDecl, new ArrayList());
							}
							((List)variableDeclarations.get(varDecl)).add(name);
						} else {
							result.add(new RemoveUnusedMemberOperation(new SimpleName[] {name}));
						}
					}
				}
			}
		}
		for (Iterator iter= variableDeclarations.keySet().iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode)iter.next();
			List names= (List)variableDeclarations.get(node);
			result.add(new RemoveUnusedMemberOperation((SimpleName[])names.toArray(new SimpleName[names.size()])));
		}
		
		if (result.size() == 0)
			return null;
		
		return new UnusedCodeFix("", compilationUnit, (IFixRewriteOperation[])result.toArray(new IFixRewriteOperation[result.size()])); //$NON-NLS-1$
	}
	
	private static boolean isFormalParameterInEnhancedForStatement(SimpleName name) {
		return name.getParent() instanceof SingleVariableDeclaration && name.getParent().getLocationInParent() == EnhancedForStatement.PARAMETER_PROPERTY;
	}
	
	private static boolean isSideEffectFree(SimpleName simpleName, CompilationUnit completeRoot) {
		SimpleName nameNode= (SimpleName) NodeFinder.perform(completeRoot, simpleName.getStartPosition(), simpleName.getLength());
		SimpleName[] references= LinkedNodeFinder.findByBinding(completeRoot, nameNode.resolveBinding());
		for (int i= 0; i < references.length; i++) {
			if (hasSideEffect(references[i]))
				return false;
		}
		return true;
	}

	private static boolean hasSideEffect(SimpleName reference) {
		ASTNode parent= reference.getParent();
		while (parent instanceof QualifiedName) {
			parent= parent.getParent();
		}
		if (parent instanceof FieldAccess) {
			parent= parent.getParent();
		}

		ASTNode node= null;
		int nameParentType= parent.getNodeType();
		if (nameParentType == ASTNode.ASSIGNMENT) {
			Assignment assignment= (Assignment) parent;
			node= assignment.getRightHandSide();
		} else if (nameParentType == ASTNode.SINGLE_VARIABLE_DECLARATION) {
			SingleVariableDeclaration decl= (SingleVariableDeclaration)parent;
			node= decl.getInitializer();
			if (node == null)
				return false;
		} else if (nameParentType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= parent;
		} else {
			return false;
		}		

		ArrayList sideEffects= new ArrayList();
		node.accept(new SideEffectFinder(sideEffects));
		return sideEffects.size() > 0;
	}

	private static SimpleName getUnusedName(CompilationUnit compilationUnit, IProblemLocation problem) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);

		SimpleName name= null;
		if (selectedNode instanceof MethodDeclaration) {
			name= ((MethodDeclaration) selectedNode).getName();
		} else if (selectedNode instanceof SimpleName) {
			name= (SimpleName) selectedNode;
		}
		if (name != null) {
			return name;
		}
		return null;
	}
	
	private static String getDisplayString(SimpleName simpleName, IBinding binding) {
		String name= simpleName.getIdentifier();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return Messages.format(FixMessages.UnusedCodeFix_RemoveType_description, name);
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveConstructor_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveMethod_description, name);
				}
			case IBinding.VARIABLE:
				if (((IVariableBinding) binding).isField()) {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				} else {
					return Messages.format(FixMessages.UnusedCodeFix_RemoveFieldOrLocal_description, name);
				}
			default:
				return ""; //$NON-NLS-1$
		}
	}
	
	private static int getCleanUpFlag(IBinding binding) {
		switch (binding.getKind()) {
			case IBinding.TYPE:
				return UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_TYPES;
			case IBinding.METHOD:
				if (((IMethodBinding) binding).isConstructor()) {
					return UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_CONSTRUCTORS;
				} else {
					return UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_METHODS;
				}
			case IBinding.VARIABLE:
				if (((IVariableBinding) binding).isField()) {
					return UnusedCodeCleanUp.REMOVE_UNUSED_PRIVATE_FIELDS;
				} else {
					return UnusedCodeCleanUp.REMOVE_UNUSED_LOCAL_VARIABLES;
				}
			default:
				return 0;
		}
	}
	
	private static ImportDeclaration getImportDeclaration(IProblemLocation problem, CompilationUnit compilationUnit) {
		ASTNode selectedNode= problem.getCoveringNode(compilationUnit);
		if (selectedNode != null) {
			ASTNode node= ASTNodes.getParent(selectedNode, ASTNode.IMPORT_DECLARATION);
			if (node instanceof ImportDeclaration) {
				return (ImportDeclaration)node;
			}
		}
		return null;
	}
	
	private final int fCleanUpFlags;
	
	private UnusedCodeFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations) {
		this(name, compilationUnit, fixRewriteOperations, 0);
	}
	
	private UnusedCodeFix(String name, CompilationUnit compilationUnit, IFixRewriteOperation[] fixRewriteOperations, int cleanUpFlag) {
		super(name, compilationUnit, fixRewriteOperations);
		fCleanUpFlags= cleanUpFlag;
	}

	public UnusedCodeCleanUp getCleanUp() {
		return new UnusedCodeCleanUp(fCleanUpFlags);
	}

}
