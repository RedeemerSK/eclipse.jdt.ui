package org.eclipse.jdt.internal.ui.viewsupport.browser;

import java.util.regex.Pattern;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class CheckboxInBrowserUtil {
	private static final Pattern CHECKED_PATERN = Pattern.compile(" *checked(=['\"]\\S*?['\"])? *"); //$NON-NLS-1$

	// not made thread-safe for now since we assume all getText/setText calls are done from display thread
	public static class BrowserTextAccessor {
		private Browser browser;
		String textCache;

		public BrowserTextAccessor(BrowserInformationControl iControl) {
			// only way so far to get hold of reference to browser is to get it through LocationListener
			iControl.addLocationListener(LocationListener.changingAdapter(this::changing)); // avoid BrowserTextAccessor itself implementing LocationListener
		}

		public BrowserTextAccessor(Browser browser) {
			this.browser = browser;
		}

		boolean isInitlaized() {
			return browser != null;
		}

		String getText() {
			if (textCache == null) {
				textCache = browser.getText();
			}
			return textCache;
		}

		void setText(String text) {
			textCache = text;
		}

		public void applyChanges() {
			if (textCache != null) {
				browser.setText(textCache);
				textCache = null;
			}
		}

		private void changing(LocationEvent event) {
			if (browser == null && event.widget instanceof Browser) {
				browser = (Browser) event.widget;
			}
		}

	}

	public interface CheckboxInBrowserLocator {
		// handle interface
	}

	private static class CheckboxInBrowserLocatorImpl implements CheckboxInBrowserLocator {
		private final String checkboxId;
		private final Pattern checkboxHtmlFragment;

		public CheckboxInBrowserLocatorImpl(String checkboxId) {
			this.checkboxId= checkboxId;
			this.checkboxHtmlFragment= Pattern.compile("<input [^>]*?id=['\"]" + checkboxId + "['\"][^>]*?>"); //$NON-NLS-1$ //$NON-NLS-2$;
		}
	}

	public static CheckboxInBrowserLocator createCheckboxInBrowserLocator(String checkboxId) {
		return new CheckboxInBrowserLocatorImpl(checkboxId);
	}

	public static void toggleCheckboxInBrowser(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocator locator, boolean enabled) {
		if (browserAccessor.isInitlaized() && locator instanceof CheckboxInBrowserLocatorImpl) {
			String html = browserAccessor.getText();
			var checkboxHtmlFragment = ((CheckboxInBrowserLocatorImpl) locator).checkboxHtmlFragment;
			var matcher = checkboxHtmlFragment.matcher(html);
			if (matcher.find()) {
				matcher.region(matcher.start(), matcher.end()).usePattern(CHECKED_PATERN);
				if (enabled) {
					if (!matcher.find()) {
						int inputFragmentEnd = matcher.regionEnd() - 1;
						StringBuilder sb = new StringBuilder(html.length() + 8);
						sb.append(html, 0, inputFragmentEnd);
						sb.append(" checked"); //$NON-NLS-1$
						sb.append(html, inputFragmentEnd, html.length());
						browserAccessor.setText(sb.toString());
					} // else it's already checked in HTML
				} else if (matcher.find()) {
					StringBuilder sb = new StringBuilder(html.length() - (matcher.end() - matcher.start()));
					sb.append(html, 0, matcher.start());
					sb.append(html, matcher.end(), html.length());
					browserAccessor.setText(sb.toString());
				} // else it's already not checked in HTML
			} else {
				JavaPlugin.logErrorMessage("Unable to locate Javadoc styling checkbox '" //$NON-NLS-1$
						+ ((CheckboxInBrowserLocatorImpl) locator).checkboxId
						+ "' in HTML"); //$NON-NLS-1$
			}
		}
	}
}