package com.jme3x.jfx.injfx.processor;

import com.jme3.post.SceneProcessor;
import com.jme3.texture.FrameBuffer;
import com.jme3x.jfx.injfx.JmeToJFXApplication;
import com.jme3x.jfx.injfx.transfer.FrameTransfer;
import com.jme3x.jfx.injfx.transfer.impl.ImageFrameTransfer;
import com.sun.istack.internal.NotNull;

import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 * The implementation of the {@link SceneProcessor} for transferring content between jME and ImageView.
 */
public class ImageViewFrameTransferSceneProcessor extends AbstractFrameTransferSceneProcessor<ImageView> {

    @Override
    protected int getDestinationHeight() {
        return (int) destination.getFitHeight();
    }

    @Override
    protected int getDestinationWidth() {
        return (int) destination.getFitWidth();
    }

    @Override
    protected boolean isPreserveRatio() {
        return destination.isPreserveRatio();
    }

    @Override
    protected void bindDestination(@NotNull final JmeToJFXApplication application, @NotNull final ImageView destination,
                                   @NotNull final Node inputNode) {
        super.bindDestination(application, destination, inputNode);
        destination.setScaleY(-1.0);
    }

    @Override
    protected void bindListeners() {
        destination.fitWidthProperty().addListener(widthListener);
        destination.fitHeightProperty().addListener(heightListener);
        destination.preserveRatioProperty().addListener(rationListener);
        super.bindListeners();
    }

    @Override
    protected void unbindDestination() {
        destination.fitWidthProperty().removeListener(widthListener);
        destination.fitHeightProperty().removeListener(heightListener);
        destination.preserveRatioProperty().removeListener(rationListener);
        super.unbindDestination();
    }

    @Override
    protected FrameTransfer createFrameTransfer(final int width, final int height, @NotNull final FrameBuffer frameBuffer) {
        return new ImageFrameTransfer(destination, isMain() ? null : frameBuffer, width, height);
    }
}