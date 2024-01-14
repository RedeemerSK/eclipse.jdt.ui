package org.eclipse.jdt.internal.ui.viewsupport.browser;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.CheckboxInBrowserLocator;

public abstract class CheckboxToggleInBrowserAction extends Action {
	private final CheckboxInBrowserLocator checkboxLocator;
	protected final BrowserTextAccessor browserAccessor;

	public CheckboxToggleInBrowserAction(String text, int style, BrowserTextAccessor browserTextAccessor, String checkboxId) {
		super(text, style);
		checkboxLocator= CheckboxInBrowserUtil.createCheckboxInBrowserLocator(checkboxId);
		browserAccessor= browserTextAccessor;
	}

	protected void toggleBrowserCheckbox(boolean enabled) {
		CheckboxInBrowserUtil.toggleCheckboxInBrowser(browserAccessor, checkboxLocator, enabled);
	}

}