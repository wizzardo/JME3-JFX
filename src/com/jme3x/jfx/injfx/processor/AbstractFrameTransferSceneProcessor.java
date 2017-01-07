package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext;
import com.jme3x.jfx.injfx.JmeToJFXApplication;
import com.jme3x.jfx.injfx.input.JFXKeyInput;
import com.jme3x.jfx.injfx.input.JFXMouseInput;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.util.JFXPlatform;
import com.sun.istack.internal.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 * The base implementation of scene processor for transferring frames.
 *
 * @author JavaSaBr.
 */
public abstract class AbstractFrameTransferSceneProcessor<T extends Node> implements FrameTransferSceneProcessor {

    /**
     * The listeners.
     */
    protected final ChangeListener<? super Number> widthListener;
    protected final ChangeListener<? super Number> heightListener;
    protected final ChangeListener<? super Boolean> rationListener;

    private final AtomicBoolean reshapeNeeded;

    private RenderManager renderManager;
    private ViewPort viewPort;
    private FrameTransfer frameTransfer;

    /**
     * THe JME application.
     */
    private volatile JmeToJFXApplication application;

    /**
     * The {@link ImageView} for showing the content of jME.
     */
    protected volatile T destination;

    /**
     * The main processor is it.
     */
    private volatile boolean main;

    private int askWidth;
    private int askHeight;

    private boolean askFixAspect;
    private boolean enabled;

    public AbstractFrameTransferSceneProcessor() {
        askWidth = 1;
        askHeight = 1;
        main = true;
        reshapeNeeded = new AtomicBoolean(true);
        widthListener = (view, oldValue, newValue) -> notifyChangedWidth(newValue);
        heightListener = (view, oldValue, newValue) -> notifyChangedHeight(newValue);
        rationListener = (view, oldValue, newValue) -> notifyChangedRatio(newValue);
    }

    /**
     * Notify about that the ratio was changed.
     *
     * @param newValue the new value of the ratio.
     */
    protected void notifyChangedRatio(final Boolean newValue) {
        notifyComponentResized(getDestinationWidth(), getDestinationHeight(), newValue);
    }

    /**
     * Notify about that the height was changed.
     *
     * @param newValue the new value of the height.
     */
    protected void notifyChangedHeight(@NotNull final Number newValue) {
        notifyComponentResized(getDestinationWidth(), newValue.intValue(), isPreserveRatio());
    }

    /**
     * Notify about that the width was changed.
     *
     * @param newValue the new value of the width.
     */
    protected void notifyChangedWidth(@NotNull final Number newValue) {
        notifyComponentResized(newValue.intValue(), getDestinationHeight(), isPreserveRatio());
    }

    /**
     * Handle resizing.
     */
    protected void notifyComponentResized(int newWidth, int newHeight, boolean fixAspect) {
        newWidth = Math.max(newWidth, 1);
        newHeight = Math.max(newHeight, 1);
        if (askWidth == newWidth && askWidth == newHeight && askFixAspect == fixAspect) return;
        askWidth = newWidth;
        askHeight = newHeight;
        askFixAspect = fixAspect;
        reshapeNeeded.set(true);
    }

    @Override
    public void reshape() {
        reshapeNeeded.set(true);
    }

    /**
     * @return is preserve ratio.
     */
    protected boolean isPreserveRatio() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the destination width.
     */
    protected int getDestinationWidth() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the destination height.
     */
    protected int getDestinationHeight() {
        throw new UnsupportedOperationException();
    }

    public void bind(@NotNull final T destination, @NotNull final JmeToJFXApplication application) {
        bind(destination, application, destination);
    }

    /**
     * Bind this processor to the destination.
     *
     * @param destination the destination.
     * @param application the application.
     * @param viewPort    the view port.
     */
    public void bind(@NotNull final T destination, @NotNull final JmeToJFXApplication application,
                     @NotNull final ViewPort viewPort) {
        bind(destination, application, destination, viewPort, true);
    }


    /**
     * Bind this processor to the destination.
     *
     * @param destination the destination.
     * @param application the application.
     * @param inputNode   the input node.
     */
    public void bind(@NotNull final T destination, @NotNull final JmeToJFXApplication application,
                     @NotNull final Node inputNode) {
        final RenderManager renderManager = application.getRenderManager();
        final List<ViewPort> postViews = renderManager.getPostViews();
        if (postViews.isEmpty()) throw new RuntimeException("the list of a post view is empty.");
        bind(destination, application, inputNode, postViews.get(postViews.size() - 1), true);
    }

    /**
     * Bind this processor to the destination.
     *
     * @param application the application.
     * @param destination the destination.
     * @param inputNode   the input node.
     * @param viewPort    the view port.
     * @param main        true if this processor is main.
     */
    public void bind(@NotNull final T destination, @NotNull final JmeToJFXApplication application,
                     @NotNull final Node inputNode, @NotNull final ViewPort viewPort, final boolean main) {

        if (this.application != null) throw new RuntimeException("This process is already bonded.");

        this.enabled = true;
        this.main = main;
        this.application = application;
        this.viewPort = viewPort;
        this.viewPort.addProcessor(this);

        JFXPlatform.runInFXThread(() -> bindDestination(application, destination, inputNode));
    }

    /**
     * Bind this processor to the destination.
     *
     * @param application the application.
     * @param destination the destination.
     * @param inputNode   the input node.
     */
    protected void bindDestination(@NotNull final JmeToJFXApplication application, @NotNull final T destination,
                                   @NotNull final Node inputNode) {

        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("this call is not from JavaFX thread.");
        }

        if (isMain()) {
            final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
            final JFXMouseInput mouseInput = context.getMouseInput();
            mouseInput.bind(inputNode);
            final JFXKeyInput keyInput = context.getKeyInput();
            keyInput.bind(inputNode);
        }

        this.destination = destination;
        bindListeners();
        this.destination.setPickOnBounds(true);

        notifyComponentResized(getDestinationWidth(), getDestinationHeight(), isPreserveRatio());
    }

    /**
     * Bind listeners to current destination.
     */
    protected void bindListeners() {
    }

    /**
     * Unbind this processor from its current destination.
     */
    public void unbind() {

        if (viewPort != null) {
            viewPort.removeProcessor(this);
            viewPort = null;
        }

        JFXPlatform.runInFXThread(this::unbindDestination);
    }

    /**
     * Unbind this processor from destination.
     */
    protected void unbindDestination() {

        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("this call is not from JavaFX thread.");
        }

        if (application != null && isMain()) {
            final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
            final JFXMouseInput mouseInput = context.getMouseInput();
            mouseInput.unbind();
            final JFXKeyInput keyInput = context.getKeyInput();
            keyInput.unbind();
        }

        application = null;

        if (destination == null) return;
        unbindListeners();
        destination = null;
    }

    /**
     * Unbind all listeners from destination.
     */
    protected void unbindListeners() {
    }

    @Override
    public boolean isMain() {
        return main;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @NotNull
    protected FrameTransfer reshapeInThread(final int width, final int height, final boolean fixAspect) {
        reshapeCurrentViewPort(width, height);

        final FrameBuffer frameBuffer = viewPort.getOutputFrameBuffer();
        final FrameTransfer frameTransfer = createFrameTransfer(width, height, frameBuffer);
        frameTransfer.initFor(renderManager.getRenderer(), isMain());

        if (isMain()) {
            final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
            context.setHeight(height);
            context.setWidth(width);
        }

        return frameTransfer;
    }

    @NotNull
    protected FrameTransfer createFrameTransfer(final int width, final int height,
                                                @NotNull final FrameBuffer frameBuffer) {
        throw new UnsupportedOperationException();
    }

    protected void reshapeCurrentViewPort(final int width, final int height) {

        final Camera cam = viewPort.getCamera();

        if (isMain()) {
            renderManager.notifyReshape(width, height);
            cam.setFrustumPerspective(getCameraAngle(), (float) cam.getWidth() / cam.getHeight(), 1f, 10000);
            return;
        }

        cam.resize(width, height, true);
        cam.setFrustumPerspective(getCameraAngle(), (float) cam.getWidth() / cam.getHeight(), 1f, 10000);

        final List<SceneProcessor> processors = viewPort.getProcessors();
        final Optional<SceneProcessor> any = processors.stream()
                .filter(sceneProcessor -> !(sceneProcessor instanceof FrameTransferSceneProcessor))
                .findAny();

        if (!any.isPresent()) {

            final FrameBuffer frameBuffer = new FrameBuffer(width, height, 1);
            frameBuffer.setDepthBuffer(Image.Format.Depth);
            frameBuffer.setColorBuffer(Image.Format.RGBA8);
            frameBuffer.setSrgb(true);

            viewPort.setOutputFrameBuffer(frameBuffer);
        }

        for (final SceneProcessor sceneProcessor : processors) {
            if (!sceneProcessor.isInitialized()) {
                sceneProcessor.initialize(renderManager, viewPort);
            } else {
                sceneProcessor.reshape(viewPort, width, height);
            }
        }
    }

    /**
     * @return the camera angle.
     */
    protected int getCameraAngle() {
        final String angle = System.getProperty("jfx.frame.transfer.camera.angle", "45");
        return Integer.parseInt(angle);
    }

    @Override
    public void initialize(@NotNull final RenderManager renderManager, @NotNull final ViewPort viewPort) {
        this.renderManager = renderManager;
    }

    @Override
    public void reshape(@NotNull final ViewPort viewPort, final int w, final int h) {
    }

    @Override
    public boolean isInitialized() {
        return frameTransfer != null;
    }

    @Override
    public void preFrame(final float tpf) {

    }

    @Override
    public void postQueue(@NotNull final RenderQueue renderQueue) {

    }

    @Override
    public void postFrame(@NotNull final FrameBuffer out) {
        if (!isEnabled()) return;

        if (frameTransfer != null) {
            frameTransfer.copyFrameBufferToImage(renderManager);
        }

        // for the next frame
        if (destination != null && reshapeNeeded.getAndSet(false)) {
            if (frameTransfer != null) frameTransfer.dispose();
            frameTransfer = reshapeInThread(askWidth, askHeight, askFixAspect);
        }
    }

    @Override
    public void cleanup() {
        if (frameTransfer != null) {
            frameTransfer.dispose();
            frameTransfer = null;
        }
    }
}
