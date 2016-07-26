package com.jme3x.jfx;

import com.jme3x.jfx.cursor.CursorDisplayProvider;
import com.jme3x.jfx.util.JFXEmbeddedUtils;
import com.jme3x.jfx.util.JFXPlatform;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * Fakes a top level window
 */
public class JmeJFXPanel extends JFXPanel {

    private static final Logger LOGGER = LoggerManager.getLogger(JmeJFXPanel.class);

    /**
     * Контейнер JavaFX UI.
     */
    private final JmeFxContainer jmeFxContainer;


    private volatile Object embeddedStage;
    private volatile Object embeddedScene;

    public JmeJFXPanel(final JmeFxContainer jmeFxContainer) {
        this.jmeFxContainer = jmeFxContainer;
    }

    /**
     * @return контейнер JavaFX UI.
     */
    private JmeFxContainer getJmeFxContainer() {
        return jmeFxContainer;
    }

    @Override
    public void repaint() {
        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        jmeFxContainer.paintComponent();
    }

    @Override
    public void setCursor(final Cursor cursor) {

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final CursorDisplayProvider cursorDisplayProvider = jmeFxContainer.getCursorDisplayProvider();

        if (cursorDisplayProvider != null) {
            cursorDisplayProvider.showCursor(cursor);
        }
    }

    @Override
    public void setScene(final Scene scene) {
        super.setScene(scene);

        this.embeddedStage = JFXEmbeddedUtils.getStage(this);
        this.embeddedScene = JFXEmbeddedUtils.getScene(this);

        if (scene == null) return;

        JFXEmbeddedUtils.setPixelScaleFactors(this, 1F, 1F);

        final int width = jmeFxContainer.getPictureWidth();
        final int height = jmeFxContainer.getPictureHeight();

        handleResize(width, height);

        //embeddedScene.setDragStartListener(new JmeFxDNDHandler(jmeFxContainer));
    }

    public void handleEvent(final KeyEvent event) {
        JFXPlatform.runInFXThread(() -> JFXEmbeddedUtils.sendKeyEventToFX(this, event));
    }

    public void handleEvent(final FocusEvent event) {
        JFXPlatform.runInFXThread(() -> JFXEmbeddedUtils.sendFocusEventToFX(this, event));
    }

    public void handleEvent(final MouseEvent event) {
        JFXPlatform.runInFXThread(() -> {
            JFXEmbeddedUtils.setCapturingMouse(this, true);
            JFXEmbeddedUtils.sendMouseEventToFX(this, event);
        });
    }

    public void handleMove(final int x, final  int y) {
        JFXEmbeddedUtils.setScreenX(this, x);
        JFXEmbeddedUtils.setScreenY(this, y);
        JFXPlatform.runInFXThread(() -> JFXEmbeddedUtils.sendMoveEventToFX(this));
    }

    public void handleResize(final int width, final int height) {
        if (width <= 0 || height <= 0) return;
        JFXEmbeddedUtils.setPHeight(this, height);
        JFXEmbeddedUtils.setPWidth(this, width);
        JFXPlatform.runInFXThread(() -> JFXEmbeddedUtils.sendResizeEventToFX(this));
    }

    /**
     * @return встроенная в панель сцена FX.
     */
    public Object getEmbeddedScene() {
        return embeddedScene;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        jmeFxContainer.setEnabled(enabled);
    }
}
