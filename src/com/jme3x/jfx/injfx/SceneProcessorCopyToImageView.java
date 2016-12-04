package com.jme3x.jfx.injfx;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.injfx.input.JFXKeyInput;
import com.jme3x.jfx.injfx.input.JFXMouseInput;
import com.jme3x.jfx.util.JFXPlatform;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.ImageView;

/**
 * The implementation of the {@link SceneProcessor} for transfering content between jME and
 * ImageView.
 */
public class SceneProcessorCopyToImageView implements SceneProcessor {

    /**
     * The listeners.
     */
    private final ChangeListener<? super Number> widthListener;
    private final ChangeListener<? super Number> heightListener;
    private final ChangeListener<? super Boolean> rationListener;

    private final AtomicBoolean reshapeNeeded;

    private RenderManager renderManager;
    private ViewPort latestViewPorts;

    private TransferImage transferImage;

    private volatile JmeToJFXApplication application;

    /**
     * The {@link ImageView} for showing the content of jME.
     */
    private volatile ImageView imageView;

    private int askWidth = 1;
    private int askHeight = 1;

    private boolean askFixAspect = true;

    public SceneProcessorCopyToImageView() {
        reshapeNeeded = new AtomicBoolean(true);
        widthListener = (view, oldValue, newValue) -> notifyChangedWidth(newValue);
        heightListener = (view, oldValue, newValue) -> notifyChangedHeight(newValue);
        rationListener = (view, oldValue, newValue) -> notifyChangedRatio(newValue);
    }

    /**
     * @return the {@link ImageView} for showing the content of jME.
     */
    protected ImageView getImageView() {
        return imageView;
    }

    /**
     * Notify about that the ratio was changed.
     *
     * @param newValue the new value of the ratio.
     */
    protected void notifyChangedRatio(final Boolean newValue) {
        notifyComponentResized((int) imageView.getFitWidth(), (int) imageView.getFitHeight(), newValue);
    }

    /**
     * Notify about that the height was changed.
     *
     * @param newValue the new value of the height.
     */
    protected void notifyChangedHeight(final Number newValue) {
        notifyComponentResized((int) imageView.getFitWidth(), newValue.intValue(), imageView.isPreserveRatio());
    }

    /**
     * Notify about that the width was changed.
     *
     * @param newValue the new value of the width.
     */
    protected void notifyChangedWidth(final Number newValue) {
        notifyComponentResized(newValue.intValue(), (int) imageView.getFitHeight(), imageView.isPreserveRatio());
    }

    protected void notifyComponentResized(int newWidth, int newHeight, boolean fixAspect) {
        newWidth = Math.max(newWidth, 1);
        newHeight = Math.max(newHeight, 1);
        if (askWidth == newWidth && askWidth == newHeight && askFixAspect == fixAspect) return;
        askWidth = newWidth;
        askHeight = newHeight;
        askFixAspect = fixAspect;
        reshapeNeeded.set(true);
    }

    public void bind(final ImageView imageView, final JmeToJFXApplication application) {
        unbind();

        final RenderManager renderManager = application.getRenderManager();
        final List<ViewPort> postViews = renderManager.getPostViews();
        if (postViews.isEmpty()) throw new RuntimeException("the list of a post view is empty.");

        latestViewPorts = postViews.get(postViews.size() - 1);
        latestViewPorts.addProcessor(this);

        JFXPlatform.runInFXThread(() -> bindImageView(application, imageView));
    }

    protected void bindImageView(final JmeToJFXApplication application, final ImageView imageView) {

        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("this call is not from JavaFX thread.");
        }

        this.application = application;

        final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
        final JFXMouseInput mouseInput = context.getMouseInput();
        mouseInput.bind(imageView);

        final JFXKeyInput keyInput = context.getKeyInput();
        keyInput.bind(imageView);

        this.imageView = imageView;
        this.imageView.fitWidthProperty().addListener(widthListener);
        this.imageView.fitHeightProperty().addListener(heightListener);
        this.imageView.preserveRatioProperty().addListener(rationListener);
        this.imageView.setPickOnBounds(true);

        notifyComponentResized((int) imageView.getFitWidth(), (int) imageView.getFitHeight(), imageView.isPreserveRatio());

        this.imageView.setScaleY(-1.0);
    }

    public void unbind() {

        if (latestViewPorts != null) {
            latestViewPorts.removeProcessor(this); // call this.cleanup()
            latestViewPorts = null;
        }

        JFXPlatform.runInFXThread(this::unbindImageView);
    }

    protected void unbindImageView() {

        if (!Platform.isFxApplicationThread()) {
            throw new RuntimeException("this call is not from JavaFX thread.");
        }

        if (application != null) {
            final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
            final JFXMouseInput mouseInput = context.getMouseInput();
            mouseInput.unbind();
            final JFXKeyInput keyInput = context.getKeyInput();
            keyInput.unbind();
            application = null;
        }

        if (imageView == null) return;

        imageView.fitWidthProperty().removeListener(widthListener);
        imageView.fitHeightProperty().removeListener(heightListener);
        imageView.preserveRatioProperty().removeListener(rationListener);
        imageView = null;
    }

    @Override
    public void initialize(final RenderManager renderManager, final ViewPort viewPort) {
        if (this.renderManager == null) {
            this.renderManager = renderManager;
        }
    }

    private TransferImage reshapeInThread(final int width, final int height, final boolean fixAspect) {

        final TransferImage transferImage = new TransferImage(imageView, width, height);
        transferImage.initFor(renderManager.getRenderer());

        renderManager.notifyReshape(transferImage.getWidth(), transferImage.getHeight());

        final JmeOffscreenSurfaceContext context = (JmeOffscreenSurfaceContext) application.getContext();
        context.setHeight(height);
        context.setWidth(width);

        return transferImage;
    }

    @Override
    public boolean isInitialized() {
        return transferImage != null;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(final RenderQueue renderQueue) {
    }

    @Override
    public void postFrame(final FrameBuffer out) {
        if (transferImage != null) {
            transferImage.copyFrameBufferToImage(renderManager);
        }

        // for the next frame
        if (reshapeNeeded.getAndSet(false)) {
            transferImage = reshapeInThread(askWidth, askHeight, askFixAspect);
            //TODO dispose previous transferImage ASAP (when no longer used in JavafFX thread)
        }
    }

    @Override
    public void cleanup() {
        if (transferImage != null) {
            transferImage.dispose();
            transferImage = null;
        }
    }

    @Override
    public void reshape(final ViewPort viewPort, final int width, final int height) {
    }
}