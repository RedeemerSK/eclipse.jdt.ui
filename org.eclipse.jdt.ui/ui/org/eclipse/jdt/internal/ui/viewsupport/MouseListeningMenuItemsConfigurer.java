package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.ActionContributionItem;

public final class MouseListeningMenuItemsConfigurer {

	final Runnable postHandleEventAction;
	MenuItem lastEnteredItem = null;
	MouseListeningMenuItemAction lastEnteredItemAction = null;

	public static void registerForMenu(Menu menu, Runnable postHandleEventAction) {
		var instance = new MouseListeningMenuItemsConfigurer(postHandleEventAction);
		ArmListener listener = instance::handleEvent;
		for (MenuItem item : menu.getItems()) {
			item.addArmListener(listener);
		}
		menu.addListener(SWT.Hide, instance::menuHidden);
	}

	private MouseListeningMenuItemsConfigurer(Runnable postHandleEventAction) {
		this.postHandleEventAction = postHandleEventAction;
	}

	private void handleEvent(ArmEvent event) {
		var menuItem = (MenuItem) event.widget;
		if (lastEnteredItem == menuItem) {
			return;
		}
		boolean runPostAction = false;
		if (lastEnteredItemAction != null) {
			runPostAction |= lastEnteredItemAction.mouseExit(event);
			lastEnteredItemAction = null;
		}
		lastEnteredItem = menuItem;
		if (menuItem.getData() instanceof ActionContributionItem
				&& ((ActionContributionItem) menuItem.getData()).getAction() instanceof MouseListeningMenuItemAction) {
			lastEnteredItemAction = (MouseListeningMenuItemAction) ((ActionContributionItem) menuItem.getData()).getAction();
			runPostAction |= lastEnteredItemAction.mouseEnter(event);
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	private void menuHidden(Event e) {
		boolean runPostAction = false;
		if (lastEnteredItemAction != null) {
			runPostAction = lastEnteredItemAction.mouseExit(null);
			lastEnteredItemAction = null;
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	public interface MouseListeningMenuItemAction  {

		default boolean mouseEnter(ArmEvent event) {
			return false;
		}
		default boolean mouseExit(ArmEvent event) {
			return false;
		}
	}
}