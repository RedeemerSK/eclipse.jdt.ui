/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

/**
 * @deprecated will be replaced by {@link TextMatchUpdater}
 */
class TextMatchFinder {
	
	private Map fJavaDocMatches; //ICompilationUnit -> Set of Integer
	private Map fCommentMatches; //ICompilationUnit -> Set of Integer
	private Map fStringMatches;//ICompilationUnit -> Set of Integer
	
	private IJavaSearchScope fScope;
	private RefactoringScanner fScanner;
	
	private TextMatchFinder(IJavaSearchScope scope, RefactoringScanner scanner, Map javaDocMatches, Map commentMatches, Map stringMatches){
		Assert.isNotNull(scope);
		Assert.isNotNull(scanner);
		fCommentMatches= commentMatches;
		fJavaDocMatches= javaDocMatches;
		fStringMatches= 	stringMatches;
		fScope= scope;
		fScanner= scanner;
	}

	static void findTextMatches(IProgressMonitor pm, IJavaSearchScope scope, ITextUpdating processor, TextChangeManager manager) throws JavaModelException{
		try{
			if (! isSearchingNeeded(processor))
				return;
			RefactoringScanner scanner = createScanner(processor);
			Map javaDocMatches= new HashMap();
			Map commentsMatches= new HashMap();
			Map stringMatches= new HashMap();
			findTextMatches(pm, scope, scanner, javaDocMatches, commentsMatches, stringMatches);
			int patternLength= scanner.getPattern().length();
			String newName= processor.getNewElementName();
			addMatches(manager, newName, patternLength, javaDocMatches, RefactoringCoreMessages.getString("TextMatchFinder.javadoc")); //$NON-NLS-1$
			addMatches(manager, newName, patternLength, commentsMatches, RefactoringCoreMessages.getString("TextMatchFinder.comment")); //$NON-NLS-1$
			addMatches(manager, newName, patternLength, stringMatches, RefactoringCoreMessages.getString("TextMatchFinder.string")); //$NON-NLS-1$
		} catch(JavaModelException e){
			throw e;	
		} catch (CoreException e){
			throw new JavaModelException(e);
		}
	}
	
	private static void addMatches(TextChangeManager manager, String newText, int patternLength, Map matches, String matchName) throws CoreException{
		for(Iterator iter= matches.keySet().iterator(); iter.hasNext();){
			Object key= iter.next();
			if (! (key instanceof ICompilationUnit))
				continue;
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)key);
			Set results= (Set)matches.get(cu);
			for (Iterator resultIter= results.iterator(); resultIter.hasNext();){
				int match= ((Integer)resultIter.next()).intValue();
				TextChangeCompatibility.addTextEdit(manager.get(cu), matchName, new ReplaceEdit(match, patternLength, newText));
			}
		}
	}

	private static void findTextMatches(IProgressMonitor pm, IJavaSearchScope scope, RefactoringScanner scanner, Map javaDocMatches, Map commentMatches, Map stringMatches) throws JavaModelException{
		new TextMatchFinder( scope, scanner, javaDocMatches, commentMatches, stringMatches).findTextMatches(pm);
	}	
	
	private static boolean isSearchingNeeded(ITextUpdating textUpdating){
		return textUpdating.getUpdateComments() || textUpdating.getUpdateJavaDoc() || textUpdating.getUpdateStrings();
	}
	
	private static RefactoringScanner createScanner(ITextUpdating textUpdating) {
		RefactoringScanner scanner= new RefactoringScanner();
		scanner.setAnalyzeComments(textUpdating.getUpdateComments());
		scanner.setAnalyzeJavaDoc(textUpdating.getUpdateJavaDoc());
		scanner.setAnalyzeStrings(textUpdating.getUpdateStrings());
		scanner.setPattern(textUpdating.getCurrentElementName());
		return scanner;
	}

	private void findTextMatches(IProgressMonitor pm) throws JavaModelException{	
		try{
			IProject[] projectsInScope= getProjectsInScope();
			
			pm.beginTask("", projectsInScope.length); //$NON-NLS-1$
			
			for (int i =0 ; i < projectsInScope.length; i++){
				if (pm.isCanceled())
					throw new OperationCanceledException();
				addTextMatches(projectsInScope[i], new SubProgressMonitor(pm, 1));
			}
		} finally{
			pm.done();
		}		
	}

	private IProject[] getProjectsInScope() {
		IPath[] enclosingProjects= fScope.enclosingProjectsAndJars();
		Set enclosingProjectSet= new HashSet();
		enclosingProjectSet.addAll(Arrays.asList(enclosingProjects));
		
		ArrayList projectsInScope= new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i =0 ; i < projects.length; i++){
			if (enclosingProjectSet.contains(projects[i].getFullPath()))
				projectsInScope.add(projects[i]);
		}
		
		return (IProject[]) projectsInScope.toArray(new IProject[projectsInScope.size()]);
	}

	private void addTextMatches(IResource resource, IProgressMonitor pm) throws JavaModelException{
		try{
			String task= RefactoringCoreMessages.getString("TextMatchFinder.searching") + resource.getFullPath(); //$NON-NLS-1$
			if (resource instanceof IFile){
				IJavaElement element= JavaCore.create(resource);
				// don't start task (flickering label updates; finally {pm.done()} is enough)
				if (!(element instanceof ICompilationUnit))
					return;
				if (! element.exists())
					return;
				if (! fScope.encloses(element))
					return;
				addCuTextMatches((ICompilationUnit)element);
			} else if (resource instanceof IContainer){
				IContainer container= (IContainer)resource;
				IResource[] members= container.members();
				pm.beginTask(task, members.length); //$NON-NLS-1$
				pm.subTask(task);
				for (int i = 0; i < members.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					
					addTextMatches(members[i], new SubProgressMonitor(pm, 1));
				}	
			}
		} catch (JavaModelException e){
			throw e;	
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}
	
	private void addCuTextMatches(ICompilationUnit cu) throws JavaModelException{
		fScanner.scan(cu);
		fJavaDocMatches.put(cu, fScanner.getJavaDocResults());
		fCommentMatches.put(cu, fScanner.getCommentResults());
		fStringMatches.put(cu, fScanner.getStringResults());
	}	
}
