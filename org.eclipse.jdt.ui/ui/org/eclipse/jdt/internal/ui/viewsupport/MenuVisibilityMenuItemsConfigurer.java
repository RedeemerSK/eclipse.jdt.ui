package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.function.BiFunction;

import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.ActionContributionItem;

public final class MenuVisibilityMenuItemsConfigurer {

	public static void registerForMenu(Menu menu, Runnable postHandleEventAction) {
		menu.addMenuListener(new MenuListenerImpl(menu, postHandleEventAction));
	}

	private static class MenuListenerImpl implements MenuListener {
		final Menu menu;
		final Runnable postHandleEventAction;

		private MenuListenerImpl(Menu menu, Runnable postHandleEventAction) {
			this.menu= menu;
			this.postHandleEventAction= postHandleEventAction;
		}

		@Override
		public void menuShown(MenuEvent e) {
			handleEvent(e, MenuVisibilityMenuItemAction::menuShown);
		}

		@Override
		public void menuHidden(MenuEvent e) {
			handleEvent(e, MenuVisibilityMenuItemAction::menuHidden);
		}

		private void handleEvent(MenuEvent e, BiFunction<MenuVisibilityMenuItemAction, MenuEvent, Boolean> callback) {
			boolean runPostAction= false;
			for (MenuItem item : menu.getItems()) {
				if (item.getData() instanceof ActionContributionItem
						&& ((ActionContributionItem) item.getData()).getAction() instanceof MenuVisibilityMenuItemAction listener) {
					runPostAction |= callback.apply(listener, e);
				}
			}
			if (postHandleEventAction != null && runPostAction) {
				postHandleEventAction.run();
			}
		}
	}

	public interface MenuVisibilityMenuItemAction {

		default boolean menuShown(MenuEvent e) {
			return false;
		}

		default boolean menuHidden(MenuEvent e) {
			return false;
		}
	}
}