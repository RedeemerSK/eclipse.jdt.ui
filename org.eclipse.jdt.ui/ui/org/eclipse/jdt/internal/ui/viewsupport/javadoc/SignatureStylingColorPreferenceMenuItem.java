package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.corext.util.Messages;

class SignatureStylingColorPreferenceMenuItem extends Action implements ImageDataProvider {
	private final Shell shell;
	private final Integer colorIdx;
	private final Function<Integer, RGB> colorPreferenceGetter;
	private final BiConsumer<Integer, RGB> colorPreferenceSetter;

	SignatureStylingColorPreferenceMenuItem(Shell shell, String textPrefix, Integer colorIdx, Function<Integer, RGB> colorPreferenceGetter, BiConsumer<Integer, RGB> colorPreferenceSetter) {
		super(Messages.format(textPrefix, colorIdx));
		this.shell = shell;
		this.colorIdx = colorIdx;
		this.colorPreferenceGetter = colorPreferenceGetter;
		this.colorPreferenceSetter = colorPreferenceSetter;
		setId(SignatureStylingColorPreferenceMenuItem.class.getSimpleName() + "_" + colorIdx); //$NON-NLS-1$
		setImageDescriptor(ImageDescriptor.createFromImageDataProvider(this));
	}

	@Override
	public ImageData getImageData(int zoom) {
		Image image = new Image(shell.getDisplay(), 16, 16);
		GC gc = new GC(image);

		gc.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
		gc.setBackground(new Color(shell.getDisplay(), getCurrentColor()));
		gc.fillRectangle(image.getBounds());
		gc.setLineWidth(2);
		gc.drawRectangle(image.getBounds());
		gc.dispose();

		ImageData data = image.getImageData(zoom);
		image.dispose();
		return data;
	}

	private RGB getCurrentColor() {
		return colorPreferenceGetter.apply(colorIdx);
	}

	@Override
	public void run() {
		ColorDialog colorDialog = new ColorDialog(shell);
		colorDialog.setRGB(getCurrentColor());
		RGB newColor = colorDialog.open();
		if (newColor != null) {
			colorPreferenceSetter.accept(colorIdx, newColor);
		}
	}
}