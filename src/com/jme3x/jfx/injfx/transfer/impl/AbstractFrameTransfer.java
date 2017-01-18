package com.jme3x.jfx.injfx.transfer.impl;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.util.JFXPlatform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

/**
 * The base implementation of a frame transfer.
 *
 * @author JavaSaBr
 */
public abstract class AbstractFrameTransfer<T> implements FrameTransfer {

    protected static final int RUNNING_STATE = 1;
    protected static final int WAITING_STATE = 2;
    protected static final int DISPOSING_STATE = 3;
    protected static final int DISPOSED_STATE = 4;

    protected final AtomicInteger frameState;
    protected final AtomicInteger imageState;

    protected final FrameBuffer frameBuffer;

    protected final PixelWriter pixelWriter;

    protected final ByteBuffer frameByteBuffer;
    protected final ByteBuffer byteBuffer;
    protected final byte[] imageByteBuffer;

    /**
     * The width.
     */
    private final int width;

    /**
     * The height.
     */
    private final int height;

    public AbstractFrameTransfer(@NotNull final T destination, final int width, final int height) {
        this(destination, null, width, height);
    }

    public AbstractFrameTransfer(@NotNull final T destination, @Nullable final FrameBuffer frameBuffer, final int width, final int height) {
        this.frameState = new AtomicInteger(WAITING_STATE);
        this.imageState = new AtomicInteger(WAITING_STATE);
        this.width = frameBuffer != null ? frameBuffer.getWidth() : width;
        this.height = frameBuffer != null ? frameBuffer.getHeight() : height;

        if (frameBuffer != null) {
            this.frameBuffer = frameBuffer;
        } else {
            this.frameBuffer = new FrameBuffer(width, height, 1);
            this.frameBuffer.setDepthBuffer(Image.Format.Depth);
            this.frameBuffer.setColorBuffer(Image.Format.RGBA8);
            this.frameBuffer.setSrgb(true);
        }

        frameByteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
        byteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
        imageByteBuffer = new byte[getWidth() * getHeight() * 4];
        pixelWriter = getPixelWriter(destination, this.frameBuffer, width, height);
    }

    @Override
    public void initFor(@NotNull final Renderer renderer, final boolean main) {
        if (main) renderer.setMainFrameBufferOverride(frameBuffer);
    }

    protected PixelWriter getPixelWriter(@NotNull final T destination, @NotNull final FrameBuffer frameBuffer, final int width, final int height) {
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
            renderer.readFrameBufferWithFormat(frameBuffer, frameByteBuffer, Image.Format.RGBA8);

        } finally {
            if (!frameState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
                throw new RuntimeException("unknown problem with the frame state");
            }
        }

        synchronized (byteBuffer) {
            byteBuffer.clear();
            byteBuffer.put(frameByteBuffer);
            byteBuffer.flip();
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
                if (byteBuffer.position() == byteBuffer.limit()) return;
                byteBuffer.get(imageByteBuffer);
            }

            for (int i = 0, length = width * height * 4; i < length; i += 4) {

                byte r = imageByteBuffer[i + 0];
                byte g = imageByteBuffer[i + 1];
                byte b = imageByteBuffer[i + 2];
                byte a = imageByteBuffer[i + 3];

                imageByteBuffer[i + 0] = b;
                imageByteBuffer[i + 1] = g;
                imageByteBuffer[i + 2] = r;
                imageByteBuffer[i + 3] = a;
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
     * @return the image byte buffer.
     */
    protected byte[] getImageByteBuffer() {
        return imageByteBuffer;
    }

    @Override
    public void dispose() {
        while (!frameState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        while (!imageState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        disposeImpl();
        frameState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
        imageState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
    }

    protected void disposeImpl() {
        frameBuffer.dispose();
        BufferUtils.destroyDirectBuffer(frameByteBuffer);
        BufferUtils.destroyDirectBuffer(byteBuffer);
    }
}
