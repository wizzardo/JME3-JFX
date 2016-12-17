package com.jme3x.jfx.injfx;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 * @author JavaSaBr
 */
public class JmeToJFXIntegrator {

    public static void prepareSettings(final AppSettings settings, final int framerate) {
        settings.setFullscreen(false);
        settings.setFrameRate(max(1, min(100, framerate + 20)));
        settings.setCustomRenderer(JmeOffscreenSurfaceContext.class);
    }

    public static SceneProcessorCopyToImageView startAndBind(final JmeToJFXApplication application, final ImageView imageView, final Function<Runnable, Thread> factory) {
        factory.apply(application::start).start();
        final SceneProcessorCopyToImageView processor = new SceneProcessorCopyToImageView();
        Platform.runLater(() -> application.enqueue(() -> processor.bind(imageView, application)));
        return processor;
    }

    public static SceneProcessorCopyToImageView bind(final JmeToJFXApplication application, final ImageView imageView) {
        final SceneProcessorCopyToImageView processor = new SceneProcessorCopyToImageView();
        processor.bind(imageView, application);
        return processor;
    }

    public static SceneProcessorCopyToImageView bind(final JmeToJFXApplication application, final ImageView imageView, final ViewPort viewPort) {
        final SceneProcessorCopyToImageView processor = new SceneProcessorCopyToImageView();
        processor.bind(imageView, application, viewPort);
        return processor;
    }

    public static SceneProcessorCopyToImageView bind(final JmeToJFXApplication application, final ImageView imageView, final Node inputNode) {
        final SceneProcessorCopyToImageView processor = new SceneProcessorCopyToImageView();
        processor.bind(imageView, application, inputNode);
        return processor;
    }

    public static SceneProcessorCopyToImageView bind(final JmeToJFXApplication application, final ImageView imageView, final Node inputNode, final ViewPort viewPort, final boolean main) {
        final SceneProcessorCopyToImageView processor = new SceneProcessorCopyToImageView();
        processor.bind(imageView, application, inputNode, viewPort, main);
        return processor;
    }
}
