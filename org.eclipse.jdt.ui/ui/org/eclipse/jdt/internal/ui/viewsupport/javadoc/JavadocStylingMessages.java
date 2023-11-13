package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public class JavadocStylingMessages extends NLS {

	private static final String BUNDLE_NAME= JavadocStylingMessages.class.getName();

	private JavadocStylingMessages() {
		// Do not instantiate
	}

	public static String JavadocStyling_styling_typeParamsReferencesColoring;
	public static String JavadocStyling_styling_typeParamsLevelsColoring;
	public static String JavadocStyling_styling_formatting;
	public static String JavadocStyling_styling_wrapping;

	public static String JavadocStyling_stylingMenu;
	public static String JavadocStyling_colorPreferences_menu;
	public static String JavadocStyling_colorPreferences_typeParameterReference;
	public static String JavadocStyling_colorPreferences_typeParameterLevel;
	public static String JavadocStyling_colorPreferences_resetAll;
	public static String JavadocStyling_colorPreferences_noTypeParameters;
	public static String JavadocStyling_colorPreferences_unusedTypeParameter;
	public static String JavadocStyling_stylingTooltip_prefix;
	public static String JavadocStyling_stylingTooltip_preference_off;
	public static String JavadocStyling_stylingTooltip_preference_hover;
	public static String JavadocStyling_stylingTooltip_preference_always;

	public static String JavadocStyling_stylingPreview_watermark;
	public static String JavadocStyling_stylingPreview_typeParamsReferencesColoring;
	public static String JavadocStyling_stylingPreview_typeParamsLevelsColoring;
	public static String JavadocStyling_stylingPreview_formatting;
	public static String JavadocStyling_stylingPreview_wrapping;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavadocStylingMessages.class);
	}

}
