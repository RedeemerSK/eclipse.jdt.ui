package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;

public class ToggleSignatureTypeParametersColoringAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureTypeParametersColoringAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureTypeParametersColoringAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_typeParamsReferencesColoring,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_REFERENCES_COLORING,
				JavaElementLinks::getPreferenceForTypeParamsReferencesColoring,
				JavaElementLinks::setPreferenceForTypeParamsReferencesColoring,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DVIEW_MEMBERS,
				JavaPluginImages.DESC_VIEW_MEMBERS,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_VIEW_MEMBERS));
	}

}
