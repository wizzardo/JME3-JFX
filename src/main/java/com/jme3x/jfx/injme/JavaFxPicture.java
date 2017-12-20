package com.jme3x.jfx.injme;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.injme.util.JFXUtils;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerManager;
import com.sun.javafx.embed.EmbeddedStageInterface;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link Picture} to represent javaFX UI Scene.
 *
 * @author JavaSaBr
 */
public class JavaFxPicture extends Picture {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JavaFxPicture.class);

    /**
     * The JavaFX container.
     */
    @NotNull
    private final JmeFxContainer container;

    public JavaFxPicture(@NotNull final JmeFxContainer container) {
        super("JavaFxContainer", true);
        this.container = container;
    }

    /**
     * Gets the JavaFX container.
     *
     * @return the JavaFX container.
     */
    private @NotNull JmeFxContainer getContainer() {
        return container;
    }

    @Override
    public void updateLogicalState(final float tpf) {

        final JmeFxContainer container = getContainer();
        final JmeContext jmeContext = container.getJmeContext();
        try {

            final EmbeddedStageInterface stageInterface = container.getStageInterface();
            if (stageInterface == null) {
                return;
            }

            final int windowWidth = JFXUtils.getWidth(jmeContext);
            final int windowHeight = JFXUtils.getHeight(jmeContext);

            if (windowWidth != container.getSceneWidth() || windowHeight != container.getSceneHeight()) {
                container.handleResize();
            }

            final int currentX = JFXUtils.getX(jmeContext);
            final int currentY = JFXUtils.getY(jmeContext);

            if (container.getPositionX() != currentX || container.getPositionY() != currentY) {

                if (JmeFxContainer.isDebugEnabled()) {
                    LOGGER.debug("moved window to [original: " + currentX + ", " + currentY + "]");
                }

                container.move(currentX, currentY);
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
