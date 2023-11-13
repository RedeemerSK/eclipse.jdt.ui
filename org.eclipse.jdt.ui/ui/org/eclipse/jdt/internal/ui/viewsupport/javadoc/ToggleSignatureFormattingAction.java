package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;

public class ToggleSignatureFormattingAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureFormattingAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
	super(ToggleSignatureFormattingAction.class.getSimpleName(),
			JavadocStylingMessages.JavadocStyling_styling_formatting,
			browserAccessor,
			JavaElementLinks.CHECKBOX_ID_FORMATTIG,
			JavaElementLinks::getPreferenceForFormatting,
			JavaElementLinks::setPreferenceForFormatting,
			preferenceKeyPrefix,
			JavaPluginImages.DESC_DLCL_TEMPLATE,
			JavaPluginImages.DESC_ELCL_TEMPLATE,
			new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_ELCL_TEMPLATE));
	}
}
