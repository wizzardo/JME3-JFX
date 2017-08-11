package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;
import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.injfx.JmeToJFXApplication;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.injfx.transfer.impl.CanvasFrameTransfer;

import org.jetbrains.annotations.NotNull;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;

/**
 * The implementation of the {@link SceneProcessor} for transferring content between jME and Canvas.
 *
 * @author JavaSaBr
 */
public class CanvasFrameTransferSceneProcessor extends AbstractFrameTransferSceneProcessor<Canvas> {

    @Override
    protected int getDestinationWidth() {
        return (int) destination.getWidth();
    }

    @Override
    protected int getDestinationHeight() {
        return (int) destination.getHeight();
    }

    @Override
    protected boolean isPreserveRatio() {
        return false;
    }

    @Override
    protected void bindDestination(@NotNull final JmeToJFXApplication application, @NotNull final Canvas destination,
                                   @NotNull final Node inputNode) {
        super.bindDestination(application, destination, inputNode);
        destination.setScaleY(-1.0);
    }

    @Override
    protected void bindListeners() {
        destination.widthProperty().addListener(widthListener);
        destination.heightProperty().addListener(heightListener);
        super.bindListeners();
    }

    @Override
    protected void unbindListeners() {
        destination.widthProperty().removeListener(widthListener);
        destination.heightProperty().removeListener(heightListener);
        super.unbindListeners();
    }

    @NotNull
    @Override
    protected FrameTransfer createFrameTransfer(final int width, final int height, @NotNull final FrameBuffer frameBuffer) {
        return new CanvasFrameTransfer(destination, getTransferMode(), isMain() ? null : frameBuffer, width, height);
    }
}