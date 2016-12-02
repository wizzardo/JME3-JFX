package com.jme3x.jfx.injfx;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.opencl.Context;
import com.jme3.renderer.Renderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;

import javafx.stage.Stage;

import static com.jme3x.jfx.util.JFXPlatform.runInFXThread;

/**
 * The implementation of the {@link JmeContext} for integrating to JavaFX.
 *
 * @author empirephoenix
 */
public class JmeContextOffscreenSurface implements JmeContext {

    protected final Stage window;
    protected final AppSettings settings;

    protected JmeContext backgroundContext;

    public JmeContextOffscreenSurface(final Stage window) {
        this.window = window;
        this.settings = createSettings();
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
    public MouseInput getMouseInput() {
        //return backgroundContext.getMouseInput();
        return null;
    }

    @Override
    public KeyInput getKeyInput() {
        //return backgroundContext.getKeyInput();
        return null;
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