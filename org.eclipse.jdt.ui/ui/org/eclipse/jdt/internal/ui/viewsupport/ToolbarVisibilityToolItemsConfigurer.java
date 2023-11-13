package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;

public final class ToolbarVisibilityToolItemsConfigurer {

	final ToolBar toolbar;
	final Runnable postHandleEventAction;

	public static void registerForToolBarManager(ToolBarManager tbm, Runnable postHandleEventAction) {
		var toolbar= tbm.getControl();
		var instance= new ToolbarVisibilityToolItemsConfigurer(toolbar, postHandleEventAction);
		Listener listener= instance::handleEvent;
		toolbar.getShell().addListener(SWT.Show, listener);
		toolbar.getShell().addListener(SWT.Hide, listener);
	}

	private ToolbarVisibilityToolItemsConfigurer(ToolBar toolbar, Runnable postHandleEventAction) {
		this.toolbar= toolbar;
		this.postHandleEventAction= postHandleEventAction;
	}

	private void handleEvent(Event event) {
		boolean runPostAction= false;
		BiFunction<ToolbarVisibilityToolItemAction, Event, Boolean> callback =
			switch (event.type) {
				case SWT.Show -> ToolbarVisibilityToolItemAction::toolbarShown;
				case SWT.Hide -> ToolbarVisibilityToolItemAction::toolbarHidden;
				default -> throw new IllegalArgumentException(event.toString());
			};
		for (ToolItem toolItem : toolbar.getItems()) {
			if (toolItem != null
					&& toolItem.getData() instanceof ActionContributionItem
					&& ((ActionContributionItem) toolItem.getData()).getAction() instanceof ToolbarVisibilityToolItemAction listener) {
				runPostAction |= callback.apply(listener, event);
			}
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	public interface ToolbarVisibilityToolItemAction  {

		default boolean toolbarShown(Event event) {
			return false;
		}
		default boolean toolbarHidden(Event event) {
			return false;
		}
	}
}