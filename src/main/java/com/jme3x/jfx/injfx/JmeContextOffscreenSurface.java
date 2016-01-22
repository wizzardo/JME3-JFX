package com.jme3x.jfx.injfx;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.renderer.Renderer;
import com.jme3.system.*;

public class JmeContextOffscreenSurface implements JmeContext {

    private final AppSettings settings = new AppSettings(true);

    private JmeContext actualContext;

    public JmeContextOffscreenSurface() {
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
    }

    @Override
    public void create(boolean waitFor) {
        lazyActualContext().create(waitFor);
    }

    @Override
    public void destroy(boolean waitFor) {
        if (actualContext == null) throw new IllegalStateException("Not created");
        // destroy wrapped context
        actualContext.destroy(waitFor);
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public KeyInput getKeyInput() {
        //return actualContext.getKeyInput();
        return null;
    }

    @Override
    public MouseInput getMouseInput() {
        //return actualContext.getMouseInput();
        return null;
    }

    @Override
    public Renderer getRenderer() {
        return actualContext.getRenderer();
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        this.settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        lazyActualContext().setSettings(this.settings);
    }

    @Override
    public Timer getTimer() {
        return actualContext.getTimer();
    }

    @Override
    public TouchInput getTouchInput() {
        return null;
    }

    @Override
    public Type getType() {
        return Type.OffscreenSurface;
    }

    @Override
    public boolean isCreated() {
        return actualContext != null && actualContext.isCreated();
    }

    @Override
    public boolean isRenderable() {
        return actualContext != null && actualContext.isRenderable();
    }

    private JmeContext lazyActualContext() {
        if (actualContext == null) {
            actualContext = JmeSystem.newContext(this.settings, Type.OffscreenSurface);
        }
        return actualContext;
    }

    @Override
    public void restart() {
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSystemListener(SystemListener listener) {
        lazyActualContext().setSystemListener(listener);
    }

    @Override
    public void setTitle(String title) {
        // TODO Auto-generated method stub
    }
}