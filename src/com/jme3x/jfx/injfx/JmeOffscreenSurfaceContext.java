package com.jme3x.jfx.injfx;

import com.jme3.input.JoyInput;
import com.jme3.input.TouchInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jme3x.jfx.injfx.input.JFXKeyInput;
import com.jme3x.jfx.injfx.input.JFXMouseInput;
import com.sun.istack.internal.NotNull;

import javax.annotation.Nullable;

/**
 * The implementation of the {@link JmeContext} for integrating to JavaFX.
 *
 * @author empirephoenix
 */
public class JmeOffscreenSurfaceContext implements JmeContext {

    /**
     * The settings.
     */
    protected final AppSettings settings;

    /**
     * The key input.
     */
    @NotNull
    protected final JFXKeyInput keyInput;

    /**
     * The mouse input.
     */
    @NotNull
    protected final JFXMouseInput mouseInput;

    /**
     * The current width.
     */
    private volatile int width;

    /**
     * The current height.
     */
    private volatile int height;

    /**
     * The background context.
     */
    protected JmeContext backgroundContext;

    public JmeOffscreenSurfaceContext() {
        this.keyInput = new JFXKeyInput(this);
        this.mouseInput = new JFXMouseInput(this);
        this.settings = createSettings();
        this.backgroundContext = createBackgroundContext();
        this.height = 1;
        this.width = 1;
    }

    /**
     * @return the current height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the current height.
     */
    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * @return the current width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the current width.
     */
    public void setWidth(final int width) {
        this.width = width;
    }

    /**
     * @return new settings.
     */
    @NotNull
    protected AppSettings createSettings() {
        final AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        return settings;
    }

    /**
     * @return new context/
     */
    @NotNull
    protected JmeContext createBackgroundContext() {
        return JmeSystem.newContext(settings, Type.OffscreenSurface);
    }

    @NotNull
    @Override
    public Type getType() {
        return Type.OffscreenSurface;
    }

    @Override
    public void setSettings(@NotNull final AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        this.backgroundContext.setSettings(settings);
    }

    @Override
    public void setSystemListener(@NotNull final SystemListener listener) {
        backgroundContext.setSystemListener(listener);
    }

    @NotNull
    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @NotNull
    @Override
    public Renderer getRenderer() {
        return backgroundContext.getRenderer();
    }

    @Nullable
    @Override
    public Context getOpenCLContext() {
        return null;
    }

    @NotNull
    @Override
    public JFXMouseInput getMouseInput() {
        return mouseInput;
    }

    @NotNull
    @Override
    public JFXKeyInput getKeyInput() {
        return keyInput;
    }

    @Nullable
    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Nullable
    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @NotNull
    @Override
    public Timer getTimer() {
        return backgroundContext.getTimer();
    }

    @Override
    public void setTitle(@NotNull final String title) {
    }

    @Override
    public boolean isCreated() {
        return backgroundContext != null && backgroundContext.isCreated();
    }

    @Override
    public boolean isRenderable() {
        return backgroundContext != null && backgroundContext.isRenderable();
    }

    @Override
    public void setAutoFlushFrames(final boolean enabled) {
        // TODO Auto-generated method stub
    }

    @Override
    public void create(final boolean waitFor) {
        final String render = System.getProperty("jfx.background.render", AppSettings.LWJGL_OPENGL3);
        backgroundContext.getSettings().setRenderer(render);
        backgroundContext.create(waitFor);
    }

    @Override
    public void restart() {
    }

    @Override
    public void destroy(final boolean waitFor) {
        if (backgroundContext == null) throw new IllegalStateException("Not created");
        // destroy wrapped context
        backgroundContext.destroy(waitFor);
    }
}