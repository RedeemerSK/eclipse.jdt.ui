package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ResourceBundle;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.ui.IViewPart;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.PartInitException;import org.eclipse.jdt.internal.debug.ui.display.DisplayAction;import org.eclipse.jdt.internal.debug.ui.display.DisplayView;import org.eclipse.jdt.internal.debug.ui.display.IDataDisplay;import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Displays the result of an evaluation in the java editor
 */
public class EditorDisplayAction extends DisplayAction {

	public EditorDisplayAction(ResourceBundle bundle, String prefix, IWorkbenchPart part) {
		super(bundle, prefix, part);
	}
	
	protected IDataDisplay getDataDisplay(IWorkbenchPart workbenchPart) {
		
		IWorkbenchPage page= JavaPlugin.getDefault().getActivePage();
		try {
			
			IViewPart view= view= page.showView(DisplayView.ID_DISPLAY_VIEW);
			if (view != null) {
				Object value= view.getAdapter(IDataDisplay.class);
				if (value instanceof IDataDisplay)
					return (IDataDisplay) value;
			}			
		} catch (PartInitException e) {
			MessageDialog.openError(getShell(), "Error in OpenHierarchyOnSelectionAction", e.getMessage());
		}
		
		return null;
	}
}
