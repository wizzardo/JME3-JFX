package com.jme3x.jfx.injfx.input;

import static com.ss.rlib.util.ObjectUtils.notNull;
import com.jme3.input.Input;
import com.jme3.input.RawInputListener;
import com.jme3x.jfx.injfx.ApplicationThreadExecutor;
import com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext;
import javafx.scene.Node;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The base implementation of the {@link Input} for using in the ImageView.
 *
 * @author JavaSaBr
 */
public class JFXInput implements Input {

    /**
     * The constant EXECUTOR.
     */
    @NotNull
    protected static final ApplicationThreadExecutor EXECUTOR = ApplicationThreadExecutor.getInstance();

    /**
     * The context.
     */
    @NotNull
    protected final JmeOffscreenSurfaceContext context;

    /**
     * The raw listener.
     */
    @Nullable
    protected RawInputListener listener;

    /**
     * The input node.
     */
    @Nullable
    protected Node node;

    /**
     * The scene.
     */
    @Nullable
    protected Scene scene;

    /**
     * Initializes is it.
     */
    protected boolean initialized;

    /**
     * Instantiates a new Jfx input.
     *
     * @param context the context
     */
    public JFXInput(@NotNull final JmeOffscreenSurfaceContext context) {
        this.context = context;
    }

    /**
     * Bind.
     *
     * @param node the node
     */
    public void bind(@NotNull final Node node) {
        this.node = node;
        this.scene = node.getScene();
        notNull(this.node, "ImageView can' be null");
        notNull(this.scene, "The scene of the ImageView can' be null");
    }

    /**
     * Unbind.
     */
    public void unbind() {
        this.node = null;
        this.scene = null;
    }

    @Override
    public void initialize() {
        if (isInitialized()) return;
        initializeImpl();
        initialized = true;
    }

    /**
     * Initialize.
     */
    protected void initializeImpl() {
    }

    @Override
    public void update() {
        if (!context.isRenderable()) return;
        updateImpl();
    }

    /**
     * Update.
     */
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
    public void setInputListener(@NotNull final RawInputListener listener) {
        this.listener = listener;
    }

    @Override
    public long getInputTimeNanos() {
        return System.nanoTime();
    }
}
