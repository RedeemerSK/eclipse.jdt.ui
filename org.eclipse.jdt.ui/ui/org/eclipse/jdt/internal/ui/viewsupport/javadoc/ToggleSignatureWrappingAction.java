package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;

public class ToggleSignatureWrappingAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureWrappingAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureWrappingAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_wrapping,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_WRAPPING,
				JavaElementLinks::getPreferenceForWrapping,
				JavaElementLinks::setPreferenceForWrapping,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DLCL_WRAP_ALL,
				JavaPluginImages.DESC_ELCL_WRAP_ALL,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_ELCL_WRAP_ALL));
	}
}
