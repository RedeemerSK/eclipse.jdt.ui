/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * Query that can be used when manipulating package fragment roots.
 * Depending on the context, <code>confirmManipulation</code> can be used to
 * determine wheter, for example, the package fragment root is to be deleted or
 * not or if the classpath of the referencing projects is to be updated.
 */
public interface IPackageFragmentRootManipulationQuery {
	
	public boolean confirmManipulation(IPackageFragmentRoot root, IJavaProject[] referencingProjects);
}
