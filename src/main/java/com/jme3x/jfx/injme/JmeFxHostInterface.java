/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx.injme;

import static com.jme3x.jfx.injme.JmeFxContainer.isDebugEnabled;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerManager;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.EmbeddedStageInterface;
import com.sun.javafx.embed.HostInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of {@link HostInterface} to listen requests or notifications from embedded scene.
 *
 * @author JavaSaBr
 */
public class JmeFxHostInterface implements HostInterface {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JmeFxHostInterface.class);

    /**
     * The JavaFX container.
     */
    @NotNull
    private final JmeFxContainer container;

    public JmeFxHostInterface(@NotNull final JmeFxContainer container) {
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
    public boolean grabFocus() {
        if (isDebugEnabled()) LOGGER.debug(this, "Grab focus");
        return true;
    }

    @Override
    public void repaint() {
        if (isDebugEnabled()) LOGGER.debug(this, "Repaint");
        getContainer().requestRedraw();
    }

    @Override
    public boolean requestFocus() {
        if (isDebugEnabled()) LOGGER.debug(this, "Request focus");
        return getContainer().requestFocus();
    }

    @Override
    public void setCursor(@NotNull final CursorFrame cursorFrame) {
        if (isDebugEnabled()) LOGGER.debug(cursorFrame, frame -> "Request showing cursor " + frame);
        getContainer().requestShowingCursor(cursorFrame);
    }

    @Override
    public void setEmbeddedScene(@Nullable final EmbeddedSceneInterface sceneInterface) {

        if (isDebugEnabled()) {
            if (sceneInterface == null) {
                LOGGER.debug(this, "Remove the scene interface.");
            } else {
                LOGGER.debug(sceneInterface, scene -> "Sets the scene interface " + scene);
            }
        }

        final JmeFxContainer container = getContainer();
        final EmbeddedSceneInterface currentSceneInterface = container.getSceneInterface();
        if (currentSceneInterface != null) {
            // FIXME release all things
        }

        container.setSceneInterface(sceneInterface);

        if (sceneInterface == null) {
            return;
        }

        sceneInterface.setPixelScaleFactor(container.getPixelScaleFactor());

        final int width = container.getSceneWidth();
        final int height = container.getSceneHeight();

        if (width > 0 && height > 0) {
            sceneInterface.setSize(width, height);
        }

        sceneInterface.setDragStartListener(new JmeFxDNDHandler(container));
    }

    @Override
    public void setEmbeddedStage(@Nullable final EmbeddedStageInterface stageInterface) {

        if (isDebugEnabled()) {
            if (stageInterface == null) {
                LOGGER.debug(this, "Remove the stage interface.");
            } else {
                LOGGER.debug(stageInterface, stage -> "Sets the stage interface " + stage);
            }
        }

        final JmeFxContainer container = getContainer();
        final EmbeddedStageInterface currentStageInterface = container.getStageInterface();
        if (currentStageInterface != null) {
            // FIXME release all things
        }

        container.setStageInterface(stageInterface);

        if (stageInterface == null) {
            return;
        }

        final int width = container.getSceneWidth();
        final int height = container.getSceneHeight();

        if (width > 0 && height > 0) {
            stageInterface.setSize(width, height);
        }

        stageInterface.setFocused(true, AbstractEvents.FOCUSEVENT_ACTIVATED);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (isDebugEnabled()) LOGGER.debug(enabled, val -> "Request enabled " + val);
        getContainer().requestEnabled(enabled);
    }

    @Override
    public void setPreferredSize(final int width, final int height) {

        if (isDebugEnabled()) {
            LOGGER.debug(width, height, (val1, val2) -> "Request preferred size " + val1 + "x" + val2);
        }

        getContainer().requestPrefferedSize(width, height);
    }

    @Override
    public boolean traverseFocusOut(final boolean forward) {

        if (isDebugEnabled()) {
            LOGGER.debug(forward, val -> "Called traverseFocusOut(" + val + ")");
        }

        return true;
    }

    @Override
    public void ungrabFocus() {
        if (isDebugEnabled()) {
            LOGGER.debug(this, "Ungrab focus");
        }
    }
}
