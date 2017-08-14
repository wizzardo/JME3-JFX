package com.jme3x.jfx.injfx.transfer.impl;

import static com.jme3x.jfx.injfx.processor.FrameTransferSceneProcessor.TransferMode;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.util.JFXPlatform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The base implementation of a frame transfer.
 *
 * @param <T> the type parameter
 * @author JavaSaBr
 */
public abstract class AbstractFrameTransfer<T> implements FrameTransfer {

    /**
     * The constant RUNNING_STATE.
     */
    protected static final int RUNNING_STATE = 1;
    /**
     * The constant WAITING_STATE.
     */
    protected static final int WAITING_STATE = 2;
    /**
     * The constant DISPOSING_STATE.
     */
    protected static final int DISPOSING_STATE = 3;
    /**
     * The constant DISPOSED_STATE.
     */
    protected static final int DISPOSED_STATE = 4;

    /**
     * The Frame state.
     */
    @NotNull
    protected final AtomicInteger frameState;

    /**
     * The Image state.
     */
    @NotNull
    protected final AtomicInteger imageState;

    /**
     * The Frame buffer.
     */
    @NotNull
    protected final FrameBuffer frameBuffer;

    /**
     * The Pixel writer.
     */
    @NotNull
    protected final PixelWriter pixelWriter;

    /**
     * The Frame byte buffer.
     */
    @NotNull
    protected final ByteBuffer frameByteBuffer;


    /**
     * The transfer mode.
     */
    @NotNull
    protected final TransferMode transferMode;

    /**
     * The byte buffer.
     */
    @NotNull
    protected final byte[] byteBuffer;

    /**
     * The image byte buffer.
     */
    @NotNull
    protected final byte[] imageByteBuffer;

    /**
     * The prev image byte buffer.
     */
    @NotNull
    protected final byte[] prevImageByteBuffer;

    /**
     * How many frames need to write else.
     */
    protected int frameCount;

    /**
     * The width.
     */
    private final int width;

    /**
     * The height.
     */
    private final int height;

    /**
     * Instantiates a new Abstract frame transfer.
     *
     * @param destination  the destination.
     * @param width        the width.
     * @param height       the height.
     * @param transferMode the transfer mode.
     */
    public AbstractFrameTransfer(@NotNull final T destination, final int width, final int height,
                                 @NotNull final TransferMode transferMode) {
        this(destination, transferMode, null, width, height);
    }

    /**
     * Instantiates a new Abstract frame transfer.
     *
     * @param destination  the destination.
     * @param transferMode the transfer mode.
     * @param frameBuffer  the frame buffer.
     * @param width        the width.
     * @param height       the height.
     */
    public AbstractFrameTransfer(@NotNull final T destination, @NotNull final TransferMode transferMode,
                                 @Nullable final FrameBuffer frameBuffer, final int width, final int height) {
        this.transferMode = transferMode;
        this.frameState = new AtomicInteger(WAITING_STATE);
        this.imageState = new AtomicInteger(WAITING_STATE);
        this.width = frameBuffer != null ? frameBuffer.getWidth() : width;
        this.height = frameBuffer != null ? frameBuffer.getHeight() : height;
        this.frameCount = 0;

        if (frameBuffer != null) {
            this.frameBuffer = frameBuffer;
        } else {
            this.frameBuffer = new FrameBuffer(width, height, 1);
            this.frameBuffer.setDepthBuffer(Image.Format.Depth);
            this.frameBuffer.setColorBuffer(Image.Format.RGBA8);
            this.frameBuffer.setSrgb(true);
        }

        frameByteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
        byteBuffer = new byte[getWidth() * getHeight() * 4];
        prevImageByteBuffer = new byte[getWidth() * getHeight() * 4];
        imageByteBuffer = new byte[getWidth() * getHeight() * 4];
        pixelWriter = getPixelWriter(destination, this.frameBuffer, width, height);
    }

    @Override
    public void initFor(@NotNull final Renderer renderer, final boolean main) {
        if (main) renderer.setMainFrameBufferOverride(frameBuffer);
    }

    /**
     * Gets pixel writer.
     *
     * @param destination the destination
     * @param frameBuffer the frame buffer
     * @param width       the width
     * @param height      the height
     * @return the pixel writer
     */
    protected PixelWriter getPixelWriter(@NotNull final T destination, @NotNull final FrameBuffer frameBuffer,
                                         final int width, final int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void copyFrameBufferToImage(final RenderManager renderManager) {

        while (!frameState.compareAndSet(WAITING_STATE, RUNNING_STATE)) {
            if (frameState.get() == DISPOSED_STATE) {
                return;
            }
        }

        // Convert screenshot.
        try {

            frameByteBuffer.clear();

            final Renderer renderer = renderManager.getRenderer();
            renderer.readFrameBufferWithFormat(frameBuffer, frameByteBuffer, Image.Format.BGRA8);

        } finally {
            if (!frameState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
                throw new RuntimeException("unknown problem with the frame state");
            }
        }

        synchronized (byteBuffer) {
            frameByteBuffer.get(byteBuffer);

            if (transferMode == TransferMode.ON_CHANGES) {

                final byte[] prevBuffer = getPrevImageByteBuffer();

                if (Arrays.equals(prevBuffer, byteBuffer)) {
                    if (frameCount == 0) return;
                } else {
                    frameCount = 2;
                    System.arraycopy(byteBuffer, 0, prevBuffer, 0, byteBuffer.length);
                }

                frameByteBuffer.position(0);
                frameCount--;
            }
        }

        JFXPlatform.runInFXThread(this::writeFrame);
    }

    /**
     * Write content to image.
     */
    protected void writeFrame() {

        while (!imageState.compareAndSet(WAITING_STATE, RUNNING_STATE)) {
            if (imageState.get() == DISPOSED_STATE) return;
        }

        try {

            final byte[] imageByteBuffer = getImageByteBuffer();

            synchronized (byteBuffer) {
                System.arraycopy(byteBuffer, 0, imageByteBuffer, 0, byteBuffer.length);
            }

            final PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraInstance();
            pixelWriter.setPixels(0, 0, width, height, pixelFormat, imageByteBuffer, 0, width * 4);

        } finally {
            if (!imageState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
                throw new RuntimeException("unknown problem with the image state");
            }
        }
    }

    /**
     * Get the image byte buffer.
     *
     * @return the image byte buffer.
     */
    @NotNull
    protected byte[] getImageByteBuffer() {
        return imageByteBuffer;
    }

    /**
     * Get the prev image byte buffer.
     *
     * @return the prev image byte buffer.
     */
    @NotNull
    protected byte[] getPrevImageByteBuffer() {
        return prevImageByteBuffer;
    }

    @Override
    public void dispose() {
        while (!frameState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        while (!imageState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        disposeImpl();
        frameState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
        imageState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
    }

    /**
     * Dispose.
     */
    protected void disposeImpl() {
        frameBuffer.dispose();
        BufferUtils.destroyDirectBuffer(frameByteBuffer);
    }
}
