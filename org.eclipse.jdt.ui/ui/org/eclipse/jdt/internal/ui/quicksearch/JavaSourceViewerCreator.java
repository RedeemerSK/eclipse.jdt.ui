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

import org.eclipse.swt.widgets.Composite;

import org.eclipse.text.quicksearch.ISourceViewerCreator;
import org.eclipse.text.quicksearch.SourceViewerHandle;
import org.eclipse.text.quicksearch.SourceViewerConfigurer;
import org.eclipse.text.quicksearch.SourceViewerConfigurer.ISourceViewerConstructor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;

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

	private final IPreferenceStore store;
	private final ISourceViewerConstructor viewerCreator;

	public JavaSourceViewerCreator() {
		List<IPreferenceStore> stores= new ArrayList<>(3);

		stores.add(JavaPlugin.getDefault().getCombinedPreferenceStore());
		stores.add(PlatformUI.getPreferenceStore()); // TODO needed ?

		store = new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));

		viewerCreator = (parent, ruler, styles) -> new JavaSourceViewer(parent, ruler, null, false, styles, store);
	}

	@Override
	public ISourceViewerHandle createSourceViewer(Composite parent) {
		return new SourceViewerHandle(new SourceViewerConfigurer(store, viewerCreator) {
			@Override
			protected void initialize() {
				super.initialize();
				fSourceViewer.configure(new SimpleJavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), fPreferenceStore, null, IJavaPartitions.JAVA_PARTITIONING, false));
			}
		}, parent, true);
	}

}
