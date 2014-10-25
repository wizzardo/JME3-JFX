package com.jme3x.jfx;

import javafx.application.Platform;

import org.lwjgl.opengl.Display;

import com.jme3.ui.Picture;
import com.sun.javafx.embed.EmbeddedStageInterface;

/**
 * Реализация картинки с UI для JME.
 * 
 * @author Ronn
 */
public class JavaFXPicture extends Picture {

	/** контейнер UI Java FX */
	private final JmeFxContainer container;
	
	public JavaFXPicture(JmeFxContainer container) {
		super("JavaFXContainer", true);
		this.container = container;
	}

	/**
	 * @return контейнер UI Java FX.
	 */
	private JmeFxContainer getContainer() {
		return container;
	}
	
	@Override
	public void updateLogicalState(float tpf) {

		final JmeFxContainer container = getContainer();
		final EmbeddedStageInterface currentStage = container.getStagePeer();
		
		try {

			if(currentStage == null) {
				return;
			}

			final int currentWidth = Display.getWidth();
			final int currentHeight = Display.getHeight();

			if(currentWidth != container.getPictureWidth() || currentHeight != container.getPictureHeight()) {
				container.handleResize();
			}

			final int x = Display.getX() + (Display.isFullscreen() ? 0 : container.getWindowOffsetX());
			final int y = Display.getY() + (Display.isFullscreen() ? 0 : container.getWindowOffsetY());

			if(container.getOldX() != x || container.getOldY() != y) {

				container.setOldX(x);
				container.setOldY(y);

				Platform.runLater(() -> currentStage.setLocation(x, y));
			}

		} finally {
			super.updateLogicalState(tpf);
		}
	}
}
