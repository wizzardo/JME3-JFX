package com.jme3x.jfx;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXUtils;
import com.sun.javafx.embed.EmbeddedStageInterface;

import javafx.application.Platform;

/**
 * Реализация картинки с UI для JME.
 *
 * @author Ronn
 */
public class JavaFXPicture extends Picture {

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

        final EmbeddedStageInterface currentStage = container.getStagePeer();

        try {

            if (currentStage == null) {
                return;
            }

            final int currentWidth = JFXUtils.getWidth(jmeContext);
            final int currentHeight = JFXUtils.getHeight(jmeContext);

            if (currentWidth != container.getPictureWidth() || currentHeight != container.getPictureHeight()) {
                container.handleResize();
            }

            final int x = JFXUtils.getX(jmeContext) + container.getWindowOffsetX();
            final int y = JFXUtils.getY(jmeContext) + container.getWindowOffsetY();

            if (container.getOldX() != x || container.getOldY() != y) {

                container.setOldX(x);
                container.setOldY(y);

                Platform.runLater(() -> currentStage.setLocation(x, y));
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
