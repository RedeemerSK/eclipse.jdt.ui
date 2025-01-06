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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.core.runtime.IPath;

import org.eclipse.text.quicksearch.ISourceViewerCreator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;


/**
 * A factory object for the <code>JavaSourceViewer</code>.
 * This indirection is necessary because only objects with a default
 * constructor can be created via an extension point
 * (this precludes Viewers).
 */
public class JavaSourceViewerCreator implements ISourceViewerCreator {

	IPreferenceStore store;

	public JavaSourceViewerCreator() {
		List<IPreferenceStore> stores= new ArrayList<>(3);

		stores.add(JavaPlugin.getDefault().getCombinedPreferenceStore());
		stores.add(PlatformUI.getPreferenceStore()); // TODO needed ?

		store = new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}

	@Override
	public ISourceViewerHandle createSourceViewer(Composite parent) {
//		var viewer = new JavaTextViewer(parent);
//		var viewer = new JavaMergeViewer(parent, 0, null);
//		var viewer = new SourceViewer(parent, null, SWT.LEFT_TO_RIGHT | SWT.H_SCROLL | SWT.V_SCROLL);
		var viewer = new JavaSourceViewer(parent, new CompositeRuler(), null, false, SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION, store);
		viewer.addVerticalRulerColumn(new LineNumberRulerColumn());
		viewer.configure(new SimpleJavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, IJavaPartitions.JAVA_PARTITIONING, false));
		viewer.setEditable(false);
		viewer.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		return new ISourceViewerHandle() {

			StyleRange[] matchRangers = null;

			{
				// enrich content based text styling with matches styles
				getSourceViewer().addTextPresentationListener(p -> p.mergeStyleRanges(matchRangers));
			}

			@Override
			public void setViewerInput(IDocument document, StyleRange[] matchRangers, IPath filePath) {
				this.matchRangers = matchRangers;
				viewer.setInput(document);
				applyMatchesStyles(matchRangers);
			}

			@Override
			public SourceViewer getSourceViewer() {
				return viewer;
//				return viewer.getSourceViewer();
			}
		};
	}
}
