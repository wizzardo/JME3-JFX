package com.jme3.jfx.injfx.transfer.impl;

import com.jme3.texture.FrameBuffer;
import com.jme3.jfx.injfx.processor.FrameTransferSceneProcessor.TransferMode;
import com.jme3.jfx.util.JfxPlatform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class for transferring a frame from jME to {@link ImageView}.
 *
 * @author JavaSaBr
 */
public class ImageFrameTransfer extends AbstractFrameTransfer<ImageView> {

    @Nullable
    private WritableImage writableImage;

    public ImageFrameTransfer(@NotNull final ImageView imageView, @NotNull final TransferMode transferMode, int width,
                              int height) {
        this(imageView, transferMode, null, width, height);
    }

    public ImageFrameTransfer(@NotNull final ImageView imageView, @NotNull final TransferMode transferMode,
                              @Nullable final FrameBuffer frameBuffer, final int width, final int height) {
        super(imageView, transferMode, frameBuffer, width, height);
        JfxPlatform.runInFxThread(() -> imageView.setImage(writableImage));
    }

    @Override
    protected PixelWriter getPixelWriter(@NotNull final ImageView destination, @NotNull final FrameBuffer frameBuffer,
                                         final int width, final int height) {
        writableImage = new WritableImage(width, height);
        return writableImage.getPixelWriter();
    }
}
