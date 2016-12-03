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

import javafx.stage.Stage;

import static com.jme3x.jfx.util.JFXPlatform.runInFXThread;
import static java.util.Objects.requireNonNull;

/**
 * The implementation of the {@link JmeContext} for integrating to JavaFX.
 *
 * @author empirephoenix
 */
public class JmeOffscreenSurfaceContext implements JmeContext {

    private static final ThreadLocal<Stage> STAGE_LOCAL = new ThreadLocal<>();

    public static void setLocalStage(final Stage stage) {
        STAGE_LOCAL.set(stage);
    }

    protected final Stage window;
    protected final AppSettings settings;

    protected final JFXKeyInput keyInput;
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
        this.window = STAGE_LOCAL.get();
        this.keyInput = new JFXKeyInput();
        this.mouseInput = new JFXMouseInput(this);
        requireNonNull(window, "you have to set a Stage to thread local.");
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

    protected AppSettings createSettings() {
        final AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        return settings;
    }

    protected JmeContext createBackgroundContext() {
        return JmeSystem.newContext(settings, Type.OffscreenSurface);
    }

    @Override
    public Type getType() {
        return Type.OffscreenSurface;
    }

    @Override
    public void setSettings(final AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        this.backgroundContext.setSettings(settings);
    }

    @Override
    public void setSystemListener(final SystemListener listener) {
        backgroundContext.setSystemListener(listener);
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return backgroundContext.getRenderer();
    }

    @Override
    public Context getOpenCLContext() {
        return null;
    }

    @Override
    public JFXMouseInput getMouseInput() {
        return mouseInput;
    }

    @Override
    public JFXKeyInput getKeyInput() {
        return keyInput;
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public Timer getTimer() {
        return backgroundContext.getTimer();
    }

    @Override
    public void setTitle(final String title) {
        runInFXThread(() -> window.setTitle(title));
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
        backgroundContext.getSettings().setRenderer(AppSettings.LWJGL_OPENGL3);
        backgroundContext.create(waitFor);
    }

    @Override
    public void restart() {
    }

    @Override
    public void destroy(boolean waitFor) {
        if (backgroundContext == null) throw new IllegalStateException("Not created");
        // destroy wrapped context
        backgroundContext.destroy(waitFor);
    }
}