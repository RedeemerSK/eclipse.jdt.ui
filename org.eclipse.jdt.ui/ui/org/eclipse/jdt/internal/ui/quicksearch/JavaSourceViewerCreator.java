/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Jozef Tomek - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.quicksearch;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.quicksearch.ITextViewerCreator;
import org.eclipse.text.quicksearch.SourceViewerConfigurer;
import org.eclipse.text.quicksearch.SourceViewerHandle;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.TextPresentation;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingReconciler;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;


/**
 * Creates quicksearch text viewer handles that use {@link JavaSourceViewer}.
 */
public class JavaSourceViewerCreator implements ITextViewerCreator {

	private static final IPreferenceStore store;

	// SourceViewerSemanticHighlightingManager needs some editor to work
	private final DummyJavaEditor fDummyJavaEditor = new DummyJavaEditor();

	static {
		store = new ChainedPreferenceStore(new IPreferenceStore[] {
			JavaPlugin.getDefault().getCombinedPreferenceStore(),
			PlatformUI.getPreferenceStore()
		});
	}

	@Override
	public ITextViewerHandle createTextViewer(Composite parent) {
		return new JavaSourceViewerHandle(parent);
	}

	// SourceViewerSemanticHighlightingManager needs some editor to work so we provide dummy one
	// Not extending CompilationUnitEditor is required for SemanticHighlightingReconciler.install() to work for us here
	private class DummyJavaEditor extends JavaEditor {

		public DummyJavaEditor() {
			dispose(); // removes store listeners amongst other things
		}


		@Override
		protected JavaSourceViewerConfiguration createJavaSourceViewerConfiguration() {
			// super.createJavaSourceViewerConfiguration() calls getPreferenceStore() which returns NULL after dispose()
			// and is final so we can't override it, thus copy-paste
			JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
			return new JavaSourceViewerConfiguration(
					textTools.getColorManager(), store, this, IJavaPartitions.JAVA_PARTITIONING);
		}


		@Override
		protected IJavaElement getElementAt(int offset) {
			return null;
		}


		@Override
		protected IJavaElement getCorrespondingElement(IJavaElement element) {
			return null;
		}
	}

	private class JavaSourceViewerHandle extends SourceViewerHandle<JavaSourceViewer>
		implements ITextPresentationListener {
			private final SourceViewerSemanticHighlightingManager fHighlightingManager;

			public JavaSourceViewerHandle(Composite parent) {
				super(new JavaSourceViewerConfigurer(), parent);
				fHighlightingManager = new SourceViewerSemanticHighlightingManager(parent.getDisplay());
				fHighlightingManager.install(
						fDummyJavaEditor,
						fSourceViewer,
						JavaPlugin.getDefault().getJavaTextTools().getColorManager(),
						store);
				parent.addDisposeListener(e -> {
					fHighlightingManager.uninstall();
				});
				fSourceViewer.addTextPresentationListener(this);
			}

			// triggered twice, 1st time during executing setInput(), 2nd time from
			// SemanticHighlightingPresenter.createUpdateRunnable()
			@Override
			public void applyTextPresentation(TextPresentation textPresentation) {
				// replaceStyleRanges() modifies passed ranges so we need to clone
				var ranges = new StyleRange[fMatchRanges.length];
				for (int i = 0; i < ranges.length; i++) {
					ranges[i] = (StyleRange) fMatchRanges[i].clone();
				}
				textPresentation.replaceStyleRanges(ranges);
			}

			@Override
			public void setViewerInput(IDocument document, StyleRange[] allMatchesStyles, IFile file) {
				fHighlightingManager.fRootElement = JavaCore.createCompilationUnitFrom(file);
				super.setViewerInput(document, allMatchesStyles, file);
			}
		}

	private class JavaSourceViewerConfigurer extends SourceViewerConfigurer<JavaSourceViewer>{

		public JavaSourceViewerConfigurer() {
			super((parent, ruler, styles) -> new JavaSourceViewer(parent, ruler, null, false, styles, store), store);
		}

		@Override
		protected void initialize() {
			super.initialize();
			fSourceViewer.configure(
				new SimpleJavaSourceViewerConfiguration(
					JavaPlugin.getDefault().getJavaTextTools().getColorManager(),
					fPreferenceStore,
					null,
					IJavaPartitions.JAVA_PARTITIONING,
					false));
		}
	}

	private static class SourceViewerSemanticHighlightingManager extends SemanticHighlightingManager {
		private final Display fDisplay;
		ITypeRoot fRootElement;

		public SourceViewerSemanticHighlightingManager(Display display) {
			fDisplay = display;
		}

		@Override
		protected SemanticHighlightingReconciler createSemanticHighlightingReconciler() {
			return new SemanticHighlightingReconciler() {

				@Override
				protected ITypeRoot getElement() {
					return fRootElement;
				}

				@Override
				protected Display getDisplay() {
					return fDisplay;
				}

				@Override
				protected boolean registerAsEditorReconcilingListener() {
					return false;
				}
			};
		}
	}

}
