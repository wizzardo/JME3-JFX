package com.jme3x.jfx;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXUtils;
import com.sun.javafx.embed.EmbeddedStageInterface;

import javafx.application.Platform;
import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * Реализация картинки с UI для JME.
 *
 * @author Ronn
 */
public class JavaFXPicture extends Picture {

    private static final Logger LOGGER = LoggerManager.getLogger(JavaFXPicture.class);

    /**
     * Контейнер UI Java FX.
     */
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
        final JmeContext jmeContext = container.getJmeContext();

        try {

            final EmbeddedStageInterface currentStage = container.getStagePeer();
            if (currentStage == null) return;

            final int currentWidth = JFXUtils.getWidth(jmeContext);
            final int currentHeight = JFXUtils.getHeight(jmeContext);

            if (currentWidth != container.getPictureWidth() || currentHeight != container.getPictureHeight()) {
                container.handleResize();
            }

            final int originalX = JFXUtils.getX(jmeContext);
            final int originalY = JFXUtils.getY(jmeContext);

            final int offsetX = JFXUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetX();
            final int offsetY = JFXUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetY();

            final int x = originalX + offsetX;
            final int y = originalY + offsetY;

            if (container.getOldX() != x || container.getOldY() != y) {

                if(JmeFxContainer.isDebug()) {
                    LOGGER.debug("moved window to [original: " + originalX + ", " + originalY + " offset:" + offsetX + ", " + offsetY +"]");
                }

                container.setOldX(x);
                container.setOldY(y);

                Platform.runLater(() -> currentStage.setLocation(x, y));
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
