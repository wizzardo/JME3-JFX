package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;
import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.injfx.JmeToJFXApplication;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.injfx.transfer.impl.CanvasFrameTransfer;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link SceneProcessor} for transferring content between jME and Canvas.
 *
 * @author JavaSaBr
 */
public class CanvasFrameTransferSceneProcessor extends AbstractFrameTransferSceneProcessor<Canvas> {

    @Override
    protected int getDestinationWidth() {
        return (int) getDestination().getWidth();
    }

    @Override
    protected int getDestinationHeight() {
        return (int) getDestination().getHeight();
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
        final Canvas destination = getDestination();
        destination.widthProperty().addListener(widthListener);
        destination.heightProperty().addListener(heightListener);
        super.bindListeners();
    }

    @Override
    protected void unbindListeners() {
        final Canvas destination = getDestination();
        destination.widthProperty().removeListener(widthListener);
        destination.heightProperty().removeListener(heightListener);
        super.unbindListeners();
    }

    @Override
    protected @NotNull FrameTransfer createFrameTransfer(@NotNull final FrameBuffer frameBuffer, final int width, final int height) {
        return new CanvasFrameTransfer(getDestination(), getTransferMode(), isMain() ? null : frameBuffer, width, height);
    }
}