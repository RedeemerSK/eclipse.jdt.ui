package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.MouseListeningMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.MouseListeningToolItemsConfigurer.MouseListeningToolbarItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.ReappearingMenuToolbarAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.CheckboxInBrowserLocator;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverStylingInBrowserMenuAction;

public class SignatureStylingMenuToolbarAction extends ReappearingMenuToolbarAction implements MouseListeningToolbarItemAction, MenuListener {
	private final BrowserTextAccessor browserAccessor;
	private final CheckboxInBrowserLocator previewCheckboxLocator;
	private boolean mouseExitCalled= false;

	public SignatureStylingMenuToolbarAction(Shell parent, BrowserTextAccessor browserAccessor, String preferenceKeyPrefix, Supplier<String> javadocContentSupplier) {
		this(JavadocStylingMessages.JavadocStyling_stylingMenu, JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT, browserAccessor,
				new ToggleSignatureTypeParametersColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureTypeLevelsColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureFormattingAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureWrappingAction(browserAccessor, preferenceKeyPrefix),
				// widget for following action is being removed and re-added repeatedly, see SignatureStylingColorSubMenuItem.menuShown()
				new SignatureStylingColorSubMenuItem(parent, javadocContentSupplier));
	}
	private SignatureStylingMenuToolbarAction(String text, ImageDescriptor image, BrowserTextAccessor browserAccessor, Action... actions) {
		super(text, image, actions);
		this.browserAccessor= browserAccessor;
		previewCheckboxLocator= CheckboxInBrowserUtil.createCheckboxInBrowserLocator(JavaElementLinks.CHECKBOX_ID_PREVIEW);
		setId(SignatureStylingMenuToolbarAction.class.getSimpleName());

		// make sure actions have loaded preferences for hover to work
		Stream.of(actions)
			.filter(HoverStylingInBrowserMenuAction.class::isInstance)
			.forEach(a -> ((HoverStylingInBrowserMenuAction) a).loadCurentPreference());
	}

	@Override
	public Menu getMenu(Control p) {
		if (!menuCreated()) {
			Menu retVal= super.getMenu(p);
			Runnable browserContentSetter= browserAccessor::applyChanges;
			MouseListeningMenuItemsConfigurer.registerForMenu(retVal, browserContentSetter);
			MenuVisibilityMenuItemsConfigurer.registerForMenu(retVal, browserContentSetter);
			retVal.addMenuListener(this); // must be last listener, since it commits browser text changes
			return retVal;
		} else {
			return super.getMenu(p);
		}
	}

	@Override
	public boolean mouseEnter(Event event) {
		boolean retVal= false;
		for (Action action : actions) {
			if (action instanceof HoverStylingInBrowserMenuAction menuAction) {
				retVal |= menuAction.menuButtonMouseEnter(event);
			}
		}
		mouseExitCalled= false;
		return retVal;
	}

	@Override
	public boolean mouseExit(Event event) {
		for (Action action : actions) {
			if (action instanceof HoverStylingInBrowserMenuAction menuAction) {
				menuAction.menuButtonMouseExit(event);
			}
		}
		return mouseExitCalled= true;
	}

	@Override
	public void runWithEvent(Event event) {
		// simulate opening menu with arrow
		Rectangle bounds= ((ToolItem) event.widget).getBounds();
		event.x= bounds.x;
		event.y= bounds.y + bounds.height;
		event.detail= SWT.ARROW;
		((ToolItem) event.widget).notifyListeners(SWT.Selection, event);
	}

	@Override
	public void menuShown(MenuEvent e) {
		toggleBrowserPreviewCheckbox(true);
		browserAccessor.applyChanges();
	}

	@Override
	public void menuHidden(MenuEvent e) {
		toggleBrowserPreviewCheckbox(false);
		if (mouseExitCalled) {
			// mouseExit() is not called after this when menu is being hidden after re-appearing, so trigger applyChanges() here
			browserAccessor.applyChanges();
		} // else applyChanges() will be triggered from mouseExit() that will be executed after this
	}

	private void toggleBrowserPreviewCheckbox(boolean enabled) {
		CheckboxInBrowserUtil.toggleCheckboxInBrowser(browserAccessor, previewCheckboxLocator, enabled);
	}
}