package com.jme3x.jfx;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXUtils;
import com.sun.javafx.embed.EmbeddedStageInterface;

import org.jetbrains.annotations.NotNull;

import javafx.application.Platform;
import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * The implementation of the {@link Picture} to represent javaFX UI.
 *
 * @author JavaSaBr
 */
public class JavaFXPicture extends Picture {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JavaFXPicture.class);

    /**
     * The JavaFX container.
     */
    @NotNull
    private final JmeFxContainer container;

    public JavaFXPicture(@NotNull final JmeFxContainer container) {
        super("JavaFXContainer", true);
        this.container = container;
    }

    /**
     * @return the JavaFX container.
     */
    @NotNull
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

            final int originalX = JFXUtils.getX(jmeContext);
            final int originalY = JFXUtils.getY(jmeContext);

            if (container.getOldX() != originalX || container.getOldY() != originalY) {

                if(JmeFxContainer.isDebug()) {
                    LOGGER.debug("moved window to [original: " + originalX + ", " + originalY + "]");
                }

                container.setOldX(originalX);
                container.setOldY(originalY);

                Platform.runLater(() -> currentStage.setLocation(originalX, originalY));
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
