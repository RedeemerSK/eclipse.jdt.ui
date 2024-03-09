/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.ArmListeningMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks.IStylingConfigurationListener;
import org.eclipse.jdt.internal.ui.viewsupport.MenuVisibilityMenuItemsConfigurer;
import org.eclipse.jdt.internal.ui.viewsupport.MouseListeningToolItemsConfigurer.IMouseListeningToolbarItemAction;
import org.eclipse.jdt.internal.ui.viewsupport.ReappearingMenuToolbarAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor;
import org.eclipse.jdt.internal.ui.viewsupport.browser.BrowserTextAccessor.IBrowserContentChangeListener;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserToggler;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverPreferenceStylingInBrowserAction;
import org.eclipse.jdt.internal.ui.viewsupport.browser.HoverStylingInBrowserMenuAction;

/**
 * Toolbar item action for building & presenting javadoc styling menu.
 */
public class SignatureStylingMenuToolbarAction extends ReappearingMenuToolbarAction implements IMouseListeningToolbarItemAction, MenuListener, IBrowserContentChangeListener {
	private final CheckboxInBrowserToggler previewCheckboxToggler;
	private final EnhancemnetsToggleAction enableAction= new EnhancemnetsToggleAction();
	private final Action[] enabledActions= { actions[0], actions[1], actions[2], actions[3], actions[4], enableAction };
	private final Action[] disabledActions= { enableAction } ;
	private final Action[] noStylingActions= { new NoStylingEnhancementsAction(), enableAction };
	private final Shell parent;

	private boolean mouseExitCalled= false;
	private boolean enhancementsToggled= false;

	public SignatureStylingMenuToolbarAction(Shell parent, BrowserTextAccessor browserAccessor, String preferenceKeyPrefix, Supplier<String> javadocContentSupplier) {
		super(JavadocStylingMessages.JavadocStyling_stylingMenu, JavaPluginImages.DESC_ETOOL_JDOC_HOVER_EDIT,
				new ToggleSignatureTypeParametersColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureTypeLevelsColoringAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureFormattingAction(browserAccessor, preferenceKeyPrefix),
				new ToggleSignatureWrappingAction(browserAccessor, preferenceKeyPrefix),
				// widget for following action is being removed and re-added repeatedly, see SignatureStylingColorSubMenuItem.menuShown()
				new SignatureStylingColorSubMenuItem(parent, javadocContentSupplier));
		this.parent= parent;
		previewCheckboxToggler= new CheckboxInBrowserToggler(browserAccessor, JavaElementLinks.CHECKBOX_ID_PREVIEW);
		setId(SignatureStylingMenuToolbarAction.class.getSimpleName());
		browserAccessor.addContentChangedListener(this); // remove not necessary since lifecycle of this action is the same as that of the browser widget
		loadActionsCurrentPreferences();
		JavaElementLinks.addStylingConfigurationListener(enableAction);
	}

	private void loadActionsCurrentPreferences() {
		Stream.of(actions)
			.filter(HoverPreferenceStylingInBrowserAction.class::isInstance)
			.forEach(a -> ((HoverPreferenceStylingInBrowserAction) a).loadCurentPreference());
	}

	@Override
	public void browserContentChanged(Supplier<String> contentAccessor) {
		if (!enableAction.isChecked()) {
			setToolTipText(JavadocStylingMessages.JavadocStyling_disabledTooltip);
			reAddActionItems(disabledActions);
		} else {
			var content= contentAccessor.get();
			if (content != null && !content.isBlank() // fail-fast
					&& previewCheckboxToggler.isCheckboxPresentInBrowser()) {
				reAddActionItems(enabledActions);
				setToolTipText(null);
			} else {
				reAddActionItems(noStylingActions);
				setToolTipText(JavadocStylingMessages.JavadocStyling_noEnhancementsTooltip);
			}
		}
	}

	@Override
	public Menu getMenu(Control p) {
		if (menu == null) {
			Menu retVal= super.getMenu(p);
			MenuVisibilityMenuItemsConfigurer.registerForMenu(retVal, previewCheckboxToggler.getBrowserTextAccessor()::applyChanges);
			retVal.addMenuListener(this); // must be last listener, since it commits browser text changes
			return retVal;
		} else {
			return super.getMenu(p);
		}
	}

	@Override
	protected void addMenuItems() {
		super.addMenuItems();
		ArmListeningMenuItemsConfigurer.registerForMenu(menu, previewCheckboxToggler.getBrowserTextAccessor()::applyChanges);
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
		if (enhancementsToggled) {
			enhancementsToggled= false;
			applyEnabledState();
		}
		toggleBrowserPreviewCheckbox(enableAction.isChecked());
		applyBrowserChanges();
	}

	@Override
	public void menuHidden(MenuEvent e) {
		// here we intentionally ignore enableAction.isChecked() to hide preview overlay when disabling enhancements
		toggleBrowserPreviewCheckbox(false);
		if (mouseExitCalled) {
			// mouseExit() is not called after this when menu is being hidden after re-appearing, so trigger applyChanges() here
			applyBrowserChanges();
		} // else applyChanges() will be triggered from mouseExit() that will be executed after this
	}

	private void toggleBrowserPreviewCheckbox(boolean enabled) {
		previewCheckboxToggler.toggleCheckboxInBrowser(enabled);
	}

	private boolean reAddActionItems(Action[] newActions) {
		if (actions != newActions) {
			actions= newActions;
			if (menu != null) {
				Stream.of(menu.getItems()).forEach(MenuItem::dispose);
				addMenuItems();
			}
			return true;
		}
		return false;
	}

	private void applyBrowserChanges() {
		previewCheckboxToggler.getBrowserTextAccessor().applyChanges();
	}

	private void applyEnabledState() {
		Consumer<HoverPreferenceStylingInBrowserAction> enabledActionsConsumer;
		if (enableAction.isChecked()) {
			loadActionsCurrentPreferences();
			enabledActionsConsumer= HoverPreferenceStylingInBrowserAction::onEnhancementsEnabled;
		} else {
			// we can't reload Javadoc that is already displayed, so we disable all enhancements to get same visual result
			enabledActionsConsumer= HoverPreferenceStylingInBrowserAction::onEnhancementsDisabled;
		}
		Stream.of(enabledActions)
			.filter(HoverPreferenceStylingInBrowserAction.class::isInstance)
			.map(HoverPreferenceStylingInBrowserAction.class::cast)
			.forEach(enabledActionsConsumer);
		// reAddActionItems() is called from browserContentChanged() executed after this
	}

	@Override
	public void setupMenuReopen(ToolBar toolbar) {
		super.setupMenuReopen(toolbar);
		toolbar.addDisposeListener(e -> JavaElementLinks.removeStylingConfigurationListener(enableAction));
	}

	private class EnhancemnetsToggleAction extends Action implements IReappearingMenuItem, IStylingConfigurationListener {
		public EnhancemnetsToggleAction() {
			super(JavadocStylingMessages.JavadocStyling_toggle, AS_CHECK_BOX);
			setChecked(JavaElementLinks.getStylingEnabledPreference());
		}

		@Override
		public void run() {
			if (isChecked()) {
				JavaElementLinks.setStylingEnabledPreference(true);
				// do changes in menuShown() that is the last callback executed in listeners execution cascade executed by SWT display thread
				enhancementsToggled= true;
			} else {
				JavaElementLinks.setStylingEnabledPreference(false);
				applyEnabledState();
				toggleBrowserPreviewCheckbox(false);
				applyBrowserChanges();
			}
		}

		@Override
		public boolean reopenMenu() {
			return isChecked();
		}

		@Override
		public void stylingStateChanged(boolean isEnabled) {
			parent.getDisplay().execute(() -> {
				if (isChecked() != isEnabled) {
					setChecked(isEnabled);
					applyEnabledState();
					applyBrowserChanges();
				}
			});
		}
	}

	private class NoStylingEnhancementsAction extends Action {
		public NoStylingEnhancementsAction() {
			super(JavadocStylingMessages.JavadocStyling_noEnhancements);
			setEnabled(false);
		}
	}
}