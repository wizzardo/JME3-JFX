package com.jme3x.jfx.injfx;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.jme3x.jfx.util.JFXPlatform;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

/**
 * The class for transferring content from the jME to {@link ImageView}.
 *
 * @author JavaSaBr.
 */
public class TransferImage {

    private static final int RUNNING_STATE = 1;
    private static final int WAITING_STATE = 2;
    private static final int DISPOSING_STATE = 3;
    private static final int DISPOSED_STATE = 4;

    private final AtomicInteger frameState;
    private final AtomicInteger imageState;

    private final FrameBuffer frameBuffer;

    private final WritableImage writableImage;

    private final ByteBuffer frameByteBuffer;
    private final ByteBuffer byteBuffer;
    private final ByteBuffer imageByteBuffer;

    /**
     * The width.
     */
    private final int width;

    /**
     * The height.
     */
    private final int height;

    public TransferImage(final ImageView imageView, final int width, final int height) {
        this.frameState = new AtomicInteger(WAITING_STATE);
        this.imageState = new AtomicInteger(WAITING_STATE);
        this.width = width;
        this.height = height;

        frameBuffer = new FrameBuffer(width, height, 1);
        frameBuffer.setDepthBuffer(Image.Format.Depth);
        frameBuffer.setColorBuffer(Image.Format.BGRA8);

        frameByteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        imageByteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        writableImage = new WritableImage(width, height);

        JFXPlatform.runInFXThread(() -> imageView.setImage(writableImage));
    }

    /**
     * Init this transfer for the render.
     *
     * @param renderer the render.
     */
    public void initFor(final Renderer renderer) {
        renderer.setMainFrameBufferOverride(frameBuffer);
    }

    /**
     * @return the width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Copy the content from render to the frameByteBuffer and write this content to image view.
     */
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
            byteBuffer.clear();
            byteBuffer.put(frameByteBuffer);
            byteBuffer.flip();
        }

        JFXPlatform.runInFXThread(this::writeImage);
    }

    /**
     * Write content to image.
     */
    private void writeImage() {
        while (!imageState.compareAndSet(WAITING_STATE, RUNNING_STATE)) {
            if (imageState.get() == DISPOSED_STATE) return;
        }

        try {

            imageByteBuffer.clear();

            synchronized (byteBuffer) {
                if (byteBuffer.position() == byteBuffer.limit()) return;
                imageByteBuffer.put(byteBuffer);
                imageByteBuffer.flip();
            }

            final WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraInstance();
            final PixelWriter pixelWriter = writableImage.getPixelWriter();
            pixelWriter.setPixels(0, 0, width, height, pixelFormat, imageByteBuffer, width * 4);
        } finally {
            if (!imageState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
                throw new RuntimeException("unknown problem with the image state");
            }
        }
    }

    /**
     * Dispose this transfer.
     */
    public void dispose() {
        while (!frameState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        while (!imageState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
        frameBuffer.dispose();
        BufferUtils.destroyDirectBuffer(frameByteBuffer);
        BufferUtils.destroyDirectBuffer(byteBuffer);
        BufferUtils.destroyDirectBuffer(imageByteBuffer);
        frameState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
        imageState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
    }
}
