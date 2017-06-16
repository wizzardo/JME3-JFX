package com.jme3x.jfx.injfx.transfer.impl;

import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.util.JFXPlatform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * The class for transferring a frame from jME to {@link ImageView}.
 *
 * @author JavaSaBr
 */
public class ImageFrameTransfer extends AbstractFrameTransfer<ImageView> {

    private WritableImage writableImage;

    /**
     * Instantiates a new Image frame transfer.
     *
     * @param imageView the image view
     * @param width     the width
     * @param height    the height
     */
    public ImageFrameTransfer(@NotNull final ImageView imageView, int width, int height) {
        this(imageView, null, width, height);
    }

    /**
     * Instantiates a new Image frame transfer.
     *
     * @param imageView   the image view
     * @param frameBuffer the frame buffer
     * @param width       the width
     * @param height      the height
     */
    public ImageFrameTransfer(@NotNull final ImageView imageView, @Nullable final FrameBuffer frameBuffer,
                              final int width, final int height) {
        super(imageView, frameBuffer, width, height);
        JFXPlatform.runInFXThread(() -> imageView.setImage(writableImage));
    }

    @Override
    protected PixelWriter getPixelWriter(@NotNull final ImageView destination, @NotNull final FrameBuffer frameBuffer,
                                         final int width, final int height) {
        writableImage = new WritableImage(width, height);
        return writableImage.getPixelWriter();
    }
}
