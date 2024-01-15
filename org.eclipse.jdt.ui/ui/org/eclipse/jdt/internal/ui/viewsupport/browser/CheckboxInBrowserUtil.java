package org.eclipse.jdt.internal.ui.viewsupport.browser;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.internal.text.html.BrowserInformationControl;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public final class CheckboxInBrowserUtil {
	private static final Pattern CHECKED_PATERN= Pattern.compile(" *checked(=['\"]\\S*?['\"])? *"); //$NON-NLS-1$

	// not made thread-safe for now since we assume all getText/setText calls are done from display thread
	public static class BrowserTextAccessor {
		private final ListenerList<BrowserContentChangeListener> listeners= new ListenerList<>();
		private Browser browser;
		private String textCache;

		public BrowserTextAccessor(BrowserInformationControl iControl) {
			// only way so far to get hold of reference to browser is to get it through LocationListener
			iControl.addLocationListener(new BrowserTextAccessorLocationListener());
		}

		public BrowserTextAccessor(Browser browser) {
			this.browser= browser;
			browser.addLocationListener(new BrowserTextAccessorLocationListener());
		}

		boolean isInitlaized() {
			return browser != null;
		}

		String getText() {
			if (textCache == null) {
				textCache= browser.getText();
			}
			return textCache;
		}

		void setText(String text) {
			textCache= text;
		}

		public void applyChanges() {
			if (textCache != null) {
				browser.setText(textCache);
				textCache= null;
			}
		}

		public void addContentChangedListener(BrowserContentChangeListener listener) {
			listeners.add(listener);
		}

		public void removeContentChangedListener(BrowserContentChangeListener listener) {
			listeners.remove(listener);
		}

		// to avoid BrowserTextAccessor itself implementing LocationListener
		private class BrowserTextAccessorLocationListener implements LocationListener {

			@Override
			public void changing(LocationEvent event) {
				if (browser == null && event.widget instanceof Browser) {
					browser= (Browser) event.widget;
				}
			}

			@Override
			public void changed(LocationEvent event) {
				textCache= null;
				listeners.forEach(listener -> listener.browserContentChanged(BrowserTextAccessor.this::getText));
			}
		}

	}

	public interface BrowserContentChangeListener {
		void browserContentChanged(Supplier<String> contentAccessor);
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

		@Override
		public String toString() {
			return "CheckboxInBrowserLocatorImpl[checkboxId: " + checkboxId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public static CheckboxInBrowserLocator createCheckboxInBrowserLocator(String checkboxId) {
		return new CheckboxInBrowserLocatorImpl(checkboxId);
	}

	public static boolean isCheckboxPresentInBrowser(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocator locator) {
		return Boolean.TRUE == actOnBrowser(browserAccessor, locator,
				// fail-fast check first
				(b, l) -> b.getText().contains(l.checkboxId) && l.checkboxHtmlFragment.matcher(b.getText()).find(),
				(b, l) -> "unable to check presence of checkbox #" + l.checkboxId); //$NON-NLS-1$
	}

	public static void toggleCheckboxInBrowser(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocator locator, boolean enabled) {
		actOnBrowser(browserAccessor, locator,
				enabled // following prevents creating new lambda instance on every call to this method
					? CheckboxInBrowserUtil::toggleCheckboxInBrowserOn
					: CheckboxInBrowserUtil::toggleCheckboxInBrowserOff,
				(b, l) -> "unable to toggle checkbox #" + l.checkboxId); //$NON-NLS-1$
	}

	private static Void toggleCheckboxInBrowserOn(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocatorImpl locator) {
		toggleCheckboxInBrowserImpl(browserAccessor, locator, true);
		return null;
	}

	private static Void toggleCheckboxInBrowserOff(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocatorImpl locator) {
		toggleCheckboxInBrowserImpl(browserAccessor, locator, false);
		return null;
	}

	private static void toggleCheckboxInBrowserImpl(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocatorImpl locator, boolean enabled) {
		String html= browserAccessor.getText();
		if (!html.contains(locator.checkboxId)) { // fail-fast check
			// browser does not currently display content with target checkbox
			return;
		}
		var matcher= locator.checkboxHtmlFragment.matcher(browserAccessor.getText());
		if (matcher.find()) {
			matcher.region(matcher.start(), matcher.end()).usePattern(CHECKED_PATERN);
			if (enabled) {
				if (!matcher.find()) {
					int inputFragmentEnd= matcher.regionEnd() - 1;
					StringBuilder sb= new StringBuilder(html.length() + 8);
					sb.append(html, 0, inputFragmentEnd);
					sb.append(" checked"); //$NON-NLS-1$
					sb.append(html, inputFragmentEnd, html.length());
					browserAccessor.setText(sb.toString());
				} // else checkbox is already checked in HTML
			} else if (matcher.find()) {
				StringBuilder sb= new StringBuilder(html.length() - (matcher.end() - matcher.start()));
				sb.append(html, 0, matcher.start());
				sb.append(html, matcher.end(), html.length());
				browserAccessor.setText(sb.toString());
			} // else checkbox is already not checked in HTML
		} // else browser does not currently display content with target checkbox
	}

	private static <R> R actOnBrowser(BrowserTextAccessor browserAccessor, CheckboxInBrowserLocator locator,
			BiFunction<BrowserTextAccessor, CheckboxInBrowserLocatorImpl, R> action,
			BiFunction<BrowserTextAccessor, CheckboxInBrowserLocatorImpl, String> notInitializedMessageSuffixSupplier) {
		if (locator instanceof CheckboxInBrowserLocatorImpl locatorImpl) {
			if (browserAccessor.isInitlaized()) {
				return action.apply(browserAccessor, locatorImpl);
			} else {
				// not really expected to happen, but if it does, let the user know, so that they can provide this info in case they decide to report
				JavaPlugin.logErrorMessage("Browser widget not yet initialized: " //$NON-NLS-1$
						+ notInitializedMessageSuffixSupplier.apply(browserAccessor, locatorImpl));
				return null;
			}
		} else { // illegal API usage
			throw new IllegalArgumentException("Passed locator is invalid"); //$NON-NLS-1$
		}
	}
}