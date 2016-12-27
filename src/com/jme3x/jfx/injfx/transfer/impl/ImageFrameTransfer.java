package com.jme3x.jfx.injfx.transfer.impl;

import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.util.JFXPlatform;
import com.sun.istack.internal.NotNull;

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

    public ImageFrameTransfer(@NotNull final ImageView imageView, @NotNull int width, int height) {
        this(imageView, null, width, height);
    }

    public ImageFrameTransfer(@NotNull final ImageView imageView, @NotNull final FrameBuffer frameBuffer, final int width, final int height) {
        super(imageView, frameBuffer, width, height);
        JFXPlatform.runInFXThread(() -> imageView.setImage(writableImage));
    }

    @Override
    protected PixelWriter getPixelWriter(@NotNull final ImageView destination, @NotNull final FrameBuffer frameBuffer, final int width, final int height) {
        writableImage = new WritableImage(width, height);
        return writableImage.getPixelWriter();
    }
}
