package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ReappearingMenuToolbarAction extends Action implements IMenuCreator {
	protected final Action[] actions;
	private Menu menu= null;
	private Point menuLocation;

	public ReappearingMenuToolbarAction(String text, ImageDescriptor image, Action... actions) {
		super(text, IAction.AS_DROP_DOWN_MENU);
		setImageDescriptor(image);
		setHoverImageDescriptor(image);
		this.actions= actions;
		setId(ReappearingMenuToolbarAction.class.getSimpleName());
		setMenuCreator(this);
	}

	public void setupMenuReopen(IToolBarManager toolbarManager) {
		for (var item : toolbarManager.getItems()) {
			if (item instanceof ActionContributionItem actionItem && actionItem.getAction() == this) {
				if (actionItem.getWidget() != null) {
					actionItem.getWidget().addListener(SWT.Selection, this::menuButtonSelected);
				} else {
					System.err.println("NULL actionItem.getWidget()"); //$NON-NLS-1$ // TODO remove
				}
				return;
			}
		}
	}

	private void menuButtonSelected(Event e) {
		if (e.detail == SWT.ARROW) {
			// menu is being shown, save location used to position the menu so that we can later show it there
			menuLocation= ((ToolItem) e.widget).getParent().toDisplay(new Point(e.x, e.y));
		}
	}

	protected boolean menuCreated() {
		return menu != null;
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu= new Menu(parent);
			Stream.of(actions).forEach(this::addMenuItem);
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	protected void addMenuItem(Action action) {
		var item= new ActionContributionItem(action);
		item.fill(menu, -1);
		((MenuItem) item.getWidget()).addSelectionListener(SelectionListener.widgetSelectedAdapter(this::itemSelected));
	}

	@SuppressWarnings("unused")
	private void itemSelected(SelectionEvent event) {
		if (menuLocation != null) {
			menu.setLocation(menuLocation);
			menu.setVisible(true); // display again after item selection
		} else {
			JavaPlugin.logErrorMessage(
					"Unable to display " //$NON-NLS-1$
					+ getClass().getName()
					+ " again since no previous display location was set"); //$NON-NLS-1$
		}
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
		}
	}

}