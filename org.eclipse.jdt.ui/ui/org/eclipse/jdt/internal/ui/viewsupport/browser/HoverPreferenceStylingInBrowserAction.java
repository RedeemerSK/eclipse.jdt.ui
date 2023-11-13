package org.eclipse.jdt.internal.ui.viewsupport.browser;

import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;

public abstract class HoverPreferenceStylingInBrowserAction extends CheckboxToggleInBrowserAction {

	protected StylingPreference currentPreference;

	public HoverPreferenceStylingInBrowserAction(String text, int style, BrowserTextAccessor browserAccessor, String checkboxId) {
		super(text, style, browserAccessor, checkboxId);
	}

	/**
	 * Enumeration of possible preferences toggling specific type of additional styling inside browser viewer.
	 */
	public enum StylingPreference {
		OFF,
		ALWAYS,
		HOVER
	}

	protected boolean isCurrentPreferenceAlways() {
		return currentPreference == StylingPreference.ALWAYS;
	}

	protected abstract StylingPreference getPreferenceFromStore();

	protected abstract void putPreferenceToStore(StylingPreference preference);

	protected abstract StylingPreference changeStylingPreference(StylingPreference oldPreference);

	protected void loadCurentPreference() {
		currentPreference= getPreferenceFromStore();
	}

	public boolean mouseEnter() {
		if (!isCurrentPreferenceAlways()) {
			toggleBrowserCheckbox(true);
			return true;
		}
		return false;
	}

	public boolean mouseExit() {
		if (!isCurrentPreferenceAlways()) {
			toggleBrowserCheckbox(false);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		currentPreference= changeStylingPreference(currentPreference);
		putPreferenceToStore(currentPreference);
	}

}