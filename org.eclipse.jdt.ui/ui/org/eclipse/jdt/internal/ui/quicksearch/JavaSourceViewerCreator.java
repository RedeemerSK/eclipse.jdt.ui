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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;


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

		stores.add(JavaPlugin.getDefault().getPreferenceStore());
		stores.add(new PreferencesAdapter(JavaPlugin.getJavaCorePluginPreferences()));
		stores.add(EditorsUI.getPreferenceStore());
		stores.add(PlatformUI.getPreferenceStore());

		store = new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}

	@Override
	public ISourceViewerHandle createSourceViewer(Composite parent) {
		var viewer = new JavaSourceViewer(parent, null, null, false, SWT.NONE, store);
		return new ISourceViewerHandle() {

			@Override
			public void setViewerInput(IDocument document, StyleRange[] matchRangers, IPath filePath) {
				viewer.setInput(document);
				applyMatchesStyles(matchRangers);
			}

			@Override
			public ITextViewer getSourceViewer() {
				return viewer;
			}
		};
	}
}
