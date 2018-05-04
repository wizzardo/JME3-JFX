package com.jme3.jfx.injfx.transfer.impl;

import com.jme3.jfx.injfx.processor.FrameTransferSceneProcessor.TransferMode;
import com.jme3.texture.FrameBuffer;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class for transferring content from the jME to {@link Canvas}.
 *
 * @author JavaSaBr
 */
public class CanvasFrameTransfer extends AbstractFrameTransfer<Canvas> {

    public CanvasFrameTransfer(@NotNull Canvas canvas, @NotNull TransferMode transferMode, int width, int height) {
        this(canvas, transferMode, null, width, height);
    }

    public CanvasFrameTransfer(
            @NotNull Canvas canvas,
            @NotNull TransferMode transferMode,
            @Nullable FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        super(canvas, transferMode, frameBuffer, width, height);
    }

    @Override
    protected PixelWriter getPixelWriter(
            @NotNull Canvas destination,
            @NotNull FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        return destination.getGraphicsContext2D().getPixelWriter();
    }
}
