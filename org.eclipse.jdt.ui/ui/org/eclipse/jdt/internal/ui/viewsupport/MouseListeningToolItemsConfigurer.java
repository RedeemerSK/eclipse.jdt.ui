package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;

public final class MouseListeningToolItemsConfigurer {

	private static final String WIDGET_DATA_KEY = MouseListeningToolItemsConfigurer.class.getSimpleName();

	final ToolBar toolbar;
	final Runnable postHandleEventAction;
	ToolItem lastEnteredItem = null;
	MouseListeningToolbarItemAction lastEnteredItemAction = null;

	public static void registerForToolBarManager(ToolBarManager tbm, Runnable postHandleEventAction) {
		var toolbar = tbm.getControl();
		if (toolbar.getData(WIDGET_DATA_KEY) == null) { // prevent re-registrations on same toolbar widget
			var instance = new MouseListeningToolItemsConfigurer(toolbar, postHandleEventAction);
			Listener listener = instance::handleEvent;
			toolbar.setData(WIDGET_DATA_KEY, listener);
			toolbar.addListener(SWT.MouseEnter, listener);
			toolbar.addListener(SWT.MouseExit, listener);
			toolbar.addListener(SWT.MouseHover, listener);
			toolbar.addListener(SWT.MouseMove, listener);
			toolbar.addListener(SWT.MouseDown, listener);
		}
	}

	private MouseListeningToolItemsConfigurer(ToolBar toolbar, Runnable postHandleEventAction) {
		this.toolbar = toolbar;
		this.postHandleEventAction = postHandleEventAction;
	}

	private void handleEvent(Event event) {
		boolean runPostAction = false;
		switch (event.type) {
			case SWT.MouseMove:
			case SWT.MouseEnter:
				var toolItem = toolbar.getItem(new Point(event.x, event.y));
				if (lastEnteredItem != toolItem) {
					if (lastEnteredItem != null) {
						runPostAction |= lastEnteredItemAction.mouseExit(event);
						lastEnteredItemAction = null;
						lastEnteredItem = null;
					}
					if (toolItem != null
							&& toolItem.getData() instanceof ActionContributionItem
							&& ((ActionContributionItem) toolItem.getData()).getAction() instanceof MouseListeningToolbarItemAction) {
						lastEnteredItem = toolItem;
						lastEnteredItemAction = (MouseListeningToolbarItemAction) ((ActionContributionItem) toolItem.getData()).getAction();
						runPostAction |= lastEnteredItemAction.mouseEnter(event);
					}
				} else if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseMove(event);
				}
				break;
			case SWT.MouseExit:
				if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseExit(event);
					lastEnteredItemAction = null;
					lastEnteredItem = null;
				}
				break;
			case SWT.MouseHover:
				if (lastEnteredItem != null) {
					runPostAction |= lastEnteredItemAction.mouseHover(event);
				}
				break;
			case SWT.MouseDown:
				if (lastEnteredItem != null && event.button == 1) {
					runPostAction |= lastEnteredItemAction.mouseClick(event);
				}
				break;
			default:
				break;
		}
		if (postHandleEventAction != null && runPostAction) {
			postHandleEventAction.run();
		}
	}

	public interface MouseListeningToolbarItemAction  {

		default boolean mouseEnter(Event event) {
			return false;
		}
		default boolean mouseExit(Event event) {
			return false;
		}
		default boolean mouseHover(Event event) {
			return false;
		}
		default boolean mouseMove(Event event) {
			return false;
		}
		default boolean mouseClick(Event event) {
			return false;
		}
	}
}