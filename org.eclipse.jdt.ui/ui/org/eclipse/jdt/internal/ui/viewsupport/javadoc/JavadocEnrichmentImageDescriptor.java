package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

class JavadocEnrichmentImageDescriptor extends CompositeImageDescriptor {
	private final ImageDescriptor baseImage;
	private final Point size;

	public JavadocEnrichmentImageDescriptor(ImageDescriptor baseImage) {
		this.baseImage = baseImage;
		CachedImageDataProvider provider = createCachedImageDataProvider(baseImage);
		size = new Point(provider.getWidth(), provider.getHeight());
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(createCachedImageDataProvider(baseImage), 0, 0);
		drawCursorOverlay();
	}

	@Override
	protected Point getSize() {
		return size;
	}

	private void drawCursorOverlay() {
		CachedImageDataProvider provider= createCachedImageDataProvider(JavaPluginImages.DESC_OVR_MOUSE_CURSOR);
		int x= size.x - provider.getWidth();
		int y= size.y - provider.getHeight();
		if (x >= 0 && y >= 0) {
			drawImage(provider, x, y);
		}
	}

}