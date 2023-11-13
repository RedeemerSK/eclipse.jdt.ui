package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;
import org.eclipse.jdt.internal.ui.viewsupport.browser.CheckboxInBrowserUtil.BrowserTextAccessor;

public class ToggleSignatureTypeLevelsColoringAction extends ToggleSignatureStylingMenuAction {

	public ToggleSignatureTypeLevelsColoringAction(BrowserTextAccessor browserAccessor, String preferenceKeyPrefix) {
		super(ToggleSignatureTypeLevelsColoringAction.class.getSimpleName(),
				JavadocStylingMessages.JavadocStyling_styling_typeParamsLevelsColoring,
				browserAccessor,
				JavaElementLinks.CHECKBOX_ID_TYPE_PARAMETERS_LEVELS_COLORING,
				JavaElementLinks::getPreferenceForTypeParamsLevelsColoring,
				JavaElementLinks::setPreferenceForTypeParamsLevelsColoring,
				preferenceKeyPrefix,
				JavaPluginImages.DESC_DVIEW_TYPES,
				JavaPluginImages.DESC_VIEW_TYPES,
				new JavadocEnrichmentImageDescriptor(JavaPluginImages.DESC_VIEW_TYPES));
	}
}
