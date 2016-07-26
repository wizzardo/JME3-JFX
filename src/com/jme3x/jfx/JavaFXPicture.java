package com.jme3x.jfx;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXWindowsUtils;

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

            final JmeJFXPanel panel = container.getHostContainer();
            if (panel == null) return;

            final int currentWidth = JFXWindowsUtils.getWidth(jmeContext);
            final int currentHeight = JFXWindowsUtils.getHeight(jmeContext);

            if (currentWidth != container.getPictureWidth() || currentHeight != container.getPictureHeight()) {
                container.handleResize();
            }

            final int originalX = JFXWindowsUtils.getX(jmeContext);
            final int originalY = JFXWindowsUtils.getY(jmeContext);

            final int offsetX = JFXWindowsUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetX();
            final int offsetY = JFXWindowsUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetY();

            final int x = originalX + offsetX;
            final int y = originalY + offsetY;

            if (container.getOldX() != x || container.getOldY() != y) {

                if(JmeFxContainer.isDebug()) {
                    LOGGER.debug("moved window to [original: " + originalX + ", " + originalY + " offset:" + offsetX + ", " + offsetY +"]");
                }

                container.setOldX(x);
                container.setOldY(y);

                panel.handleMove(x, y);
            }

        } finally {
            super.updateLogicalState(tpf);
        }
    }
}
