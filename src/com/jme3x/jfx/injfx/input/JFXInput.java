package com.jme3x.jfx.injfx.input;

import com.jme3.input.Input;
import com.jme3.input.RawInputListener;
import com.jme3x.jfx.injfx.ApplicationThreadExecutor;
import com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext;

import java.util.Objects;

import javafx.scene.Scene;
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

    protected Scene scene;

    protected boolean initialized;

    public JFXInput(final JmeOffscreenSurfaceContext context) {
        this.context = context;
    }

    public void bind(final ImageView imageView) {
        this.imageView = imageView;
        this.scene = imageView.getScene();
        Objects.requireNonNull(this.imageView, "ImageView can' be null");
        Objects.requireNonNull(this.scene, "The scene of the ImageView can' be null");
    }

    public void unbind() {
        this.imageView = null;
        this.scene = null;
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
        unbind();
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
