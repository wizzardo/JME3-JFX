package com.jme3x.jfx;

import com.jme3.system.JmeContext;
import com.jme3.ui.Picture;
import com.jme3x.jfx.util.JFXWindowUtils;

import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * Реализация картинки с UI для JME.
 *
 * @author Ronn
 */
public class JmeFxPicture extends Picture {

    private static final Logger LOGGER = LoggerManager.getLogger(JmeFxPicture.class);

    /**
     * Контейнер UI Java FX.
     */
    private final JmeFxContainer container;

    public JmeFxPicture(final JmeFxContainer container) {
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

            final JmeFxPanel panel = container.getHostContainer();
            if (panel == null) return;

            final int currentWidth = JFXWindowUtils.getWidth(jmeContext);
            final int currentHeight = JFXWindowUtils.getHeight(jmeContext);

            if (currentWidth != container.getPictureWidth() || currentHeight != container.getPictureHeight()) {
                container.handleResize();
            }

            final int originalX = JFXWindowUtils.getX(jmeContext);
            final int originalY = JFXWindowUtils.getY(jmeContext);

            final int offsetX = JFXWindowUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetX();
            final int offsetY = JFXWindowUtils.isFullscreen(jmeContext) ? 0 : container.getWindowOffsetY();

            final int x = originalX + offsetX;
            final int y = originalY + offsetY;

            if (container.getOldX() != x || container.getOldY() != y) {

                if (JmeFxContainer.isDebug()) {
                    LOGGER.debug("moved window to [original: " + originalX + ", " + originalY + " offset:" + offsetX + ", " + offsetY + "]");
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
