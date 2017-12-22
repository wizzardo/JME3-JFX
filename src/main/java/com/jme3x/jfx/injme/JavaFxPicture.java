package com.jme3x.jfx.injme;

import static com.jme3x.jfx.injme.util.JmeWindowUtils.*;
import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXPlatform;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerLevel;
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
    private static final Logger LOGGER = LoggerManager.getLogger(JFXPlatform.class);

    /**
     * The JavaFX container.
     */
    @NotNull
    private final JmeFXContainerInternal container;

    public JavaFxPicture(@NotNull final JmeFXContainerInternal container) {
        super("JavaFxContainer", true);
        this.container = container;
    }

    /**
     * Gets the JavaFX container.
     *
     * @return the JavaFX container.
     */
    private @NotNull JmeFXContainerInternal getContainer() {
        return container;
    }

    @Override
    public void updateLogicalState(final float tpf) {

        final JmeFXContainerInternal container = getContainer();
        final JmeContext jmeContext = container.getJmeContext();
        try {

            final EmbeddedStageInterface stageInterface = container.getStageInterface();
            if (stageInterface == null) {
                return;
            }

            final int windowWidth = getWidth(jmeContext);
            final int windowHeight = getHeight(jmeContext);

            if (windowWidth != container.getSceneWidth() || windowHeight != container.getSceneHeight()) {
                container.fitSceneToWindowSize();
            }

            final int currentX = getX(jmeContext);
            final int currentY = getY(jmeContext);

            if (container.getPositionX() != currentX || container.getPositionY() != currentY) {

                if (LOGGER.isEnabled(LoggerLevel.DEBUG)) {
                    LOGGER.debug(this, "moved window to [original: " + currentX + ", " + currentY + "]");
                }

                container.move(currentX, currentY);
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
