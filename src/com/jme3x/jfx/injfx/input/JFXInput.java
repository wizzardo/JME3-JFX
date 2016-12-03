package com.jme3x.jfx.injfx.input;

import com.jme3.input.Input;
import com.jme3.input.RawInputListener;
import com.jme3x.jfx.injfx.ApplicationThreadExecutor;
import com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext;

import javafx.scene.image.ImageView;

/**
 * The base implementation of the {@link Input} for using in the ImageView.
 *
 * @author JavaSaBr.
 */
public class JFXInput implements Input {

    protected static final ApplicationThreadExecutor EXECUTOR = ApplicationThreadExecutor.getInstance();

    protected final JmeOffscreenSurfaceContext context;

    protected RawInputListener listener;

    protected ImageView imageView;

    protected boolean initialized;

    public JFXInput(final JmeOffscreenSurfaceContext context) {
        this.context = context;
    }

    public void bind(final ImageView imageView) {
    }

    public void unbind() {
    }

    @Override
    public void initialize() {
        if (isInitialized()) return;
        initializeImpl();
        initialized = true;
    }

    protected void initializeImpl() {

    }

    @Override
    public void update() {
        if (!context.isRenderable()) return;
        updateImpl();
    }

    protected void updateImpl() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInputListener(final RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }
}
