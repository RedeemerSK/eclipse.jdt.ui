/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.quicksearch;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IPath;

import org.eclipse.text.quicksearch.ISourceViewerCreator;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jdt.internal.ui.compare.JavaTextViewer;


/**
 * A factory object for the <code>JavaTextViewer</code>.
 * This indirection is necessary because only objects with a default
 * constructor can be created via an extension point
 * (this precludes Viewers).
 */
public class JavaTextViewerCreator implements ISourceViewerCreator {

	@Override
	public ISourceViewerHandle createSourceViewer(Composite parent) {
		var viewer = new JavaTextViewer(parent);
		return new ISourceViewerHandle() {

			@Override
			public void setViewerInput(IDocument document, StyleRange[] matchRangers, IPath filePath) {
				viewer.setInput(document);
				applyMatchesStyles(matchRangers);
			}

			@Override
			public ITextViewer getSourceViewer() {
				return viewer.getSourceViewer();
			}
		};
	}
}
