package com.jme3x.jfx.injfx;

import static java.lang.Math.max;
import static java.lang.Math.min;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;
import com.jme3x.jfx.injfx.processor.CanvasFrameTransferSceneProcessor;
import com.jme3x.jfx.injfx.processor.FrameTransferSceneProcessor;
import com.jme3x.jfx.injfx.processor.ImageViewFrameTransferSceneProcessor;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * The type Jme to jfx integrator.
 *
 * @author JavaSaBr
 */
public class JmeToJFXIntegrator {

    /**
     * Prepare settings.
     *
     * @param settings  the settings
     * @param frameRate the frame rate
     */
    @NotNull
    public static AppSettings prepareSettings(@NotNull final AppSettings settings, final int frameRate) {
        settings.setFullscreen(false);
        settings.setFrameRate(max(1, min(100, frameRate)));
        settings.setCustomRenderer(JmeOffscreenSurfaceContext.class);
        return settings;
    }

    /**
     * Start and bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @param factory     the factory
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor startAndBind(@NotNull final JmeToJFXApplication application,
                                                           @NotNull final ImageView imageView, @NotNull final Function<Runnable, Thread> factory) {
        factory.apply(application::start).start();
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.setTransferMode(FrameTransferSceneProcessor.TransferMode.ON_CHANGES);
        Platform.runLater(() -> application.enqueue(() -> processor.bind(imageView, application)));
        return processor;
    }

    /**
     * Start and bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @param factory     the factory
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor startAndBindMainViewPort(@NotNull final JmeToJFXApplication application,
                                                                       @NotNull final ImageView imageView,
                                                                       @NotNull final Function<Runnable, Thread> factory) {
        factory.apply(application::start).start();
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.setTransferMode(FrameTransferSceneProcessor.TransferMode.ON_CHANGES);
        application.enqueue(() -> processor.bind(imageView, application, application.getViewPort()));

        return processor;
    }

    /**
     * Start and bind frame transfer scene processor.
     *
     * @param application the application
     * @param canvas      the canvas
     * @param factory     the factory
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor startAndBind(@NotNull final JmeToJFXApplication application,
                                                           @NotNull final Canvas canvas, @NotNull final Function<Runnable, Thread> factory) {
        factory.apply(application::start).start();
        final CanvasFrameTransferSceneProcessor processor = new CanvasFrameTransferSceneProcessor();
        Platform.runLater(() -> application.enqueue(() -> processor.bind(canvas, application)));
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application, @NotNull final ImageView imageView) {
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.bind(imageView, application);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param canvas      the canvas
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application, @NotNull final Canvas canvas) {
        final CanvasFrameTransferSceneProcessor processor = new CanvasFrameTransferSceneProcessor();
        processor.bind(canvas, application);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @param viewPort    the view port
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final ImageView imageView, @NotNull final ViewPort viewPort) {
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.bind(imageView, application, viewPort);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param canvas      the canvas
     * @param viewPort    the view port
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final Canvas canvas, @NotNull final ViewPort viewPort) {
        final CanvasFrameTransferSceneProcessor processor = new CanvasFrameTransferSceneProcessor();
        processor.bind(canvas, application, viewPort);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @param inputNode   the input node
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final ImageView imageView, @NotNull final Node inputNode) {
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.bind(imageView, application, inputNode);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param canvas      the canvas
     * @param inputNode   the input node
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final Canvas canvas, @NotNull final Node inputNode) {
        final CanvasFrameTransferSceneProcessor processor = new CanvasFrameTransferSceneProcessor();
        processor.bind(canvas, application, inputNode);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param imageView   the image view
     * @param inputNode   the input node
     * @param viewPort    the view port
     * @param main        the main
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final ImageView imageView,
                                                   @NotNull final Node inputNode,
                                                   @NotNull final ViewPort viewPort, final boolean main) {
        final ImageViewFrameTransferSceneProcessor processor = new ImageViewFrameTransferSceneProcessor();
        processor.bind(imageView, application, inputNode, viewPort, main);
        return processor;
    }

    /**
     * Bind frame transfer scene processor.
     *
     * @param application the application
     * @param canvas      the canvas
     * @param inputNode   the input node
     * @param viewPort    the view port
     * @param main        the main
     * @return the frame transfer scene processor
     */
    @NotNull
    public static FrameTransferSceneProcessor bind(@NotNull final JmeToJFXApplication application,
                                                   @NotNull final Canvas canvas,
                                                   @NotNull final Node inputNode,
                                                   @NotNull final ViewPort viewPort, final boolean main) {
        final CanvasFrameTransferSceneProcessor processor = new CanvasFrameTransferSceneProcessor();
        processor.bind(canvas, application, inputNode, viewPort, main);
        return processor;
    }
}
