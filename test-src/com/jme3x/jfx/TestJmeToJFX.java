package com.jme3x.jfx;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.NativeLibraryLoader;
import com.jme3x.jfx.injfx.JmeContextOffscreenSurface;
import com.jme3x.jfx.injfx.SceneProcessorCopyToImageView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by ronn on 03.12.16.
 */
public class TestJmeToJFX extends Application {

    private SceneProcessorCopyToImageView jmeAppDisplayBinder = new SceneProcessorCopyToImageView();

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {

        final ImageView imageView = new ImageView();
        final Button button = new Button("BUTTON");
        final StackPane stackPane = new StackPane(imageView, button);
        final Scene scene = new Scene(stackPane, 600, 600);

        imageView.fitWidthProperty().bind(stackPane.widthProperty());
        imageView.fitHeightProperty().bind(stackPane.heightProperty());

        stage.setTitle("Test");
        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            final SimpleApplication application = makeJmeApplication(stage, 80);
            new Thread(application::start).start();

            Platform.runLater(() -> {
                application.enqueue(() -> jmeAppDisplayBinder.bind(imageView, application));
            });
        }).start();
    }


    private static SimpleApplication makeJmeApplication(Stage stage, int framerate) {
        AppSettings settings = new AppSettings(true);
        // important to use those settings
        settings.setFullscreen(false);
        settings.setUseInput(false);
        settings.setFrameRate(Math.max(1, Math.min(80, framerate)));
        settings.setCustomRenderer(com.jme3x.jfx.injfx.JmeContextOffscreenSurface.class);

        SimpleApplication app = new SimpleApplication(){

            public Geometry player;

            @Override
            public void start() {

                if ("LWJGL".equals(settings.getAudioRenderer())) {
                    NativeLibraryLoader.loadNativeLibrary("openal-lwjgl3", true);
                }

                NativeLibraryLoader.loadNativeLibrary("lwjgl3", true);
                NativeLibraryLoader.loadNativeLibrary("glfw-lwjgl3", true);
                NativeLibraryLoader.loadNativeLibrary("jemalloc-lwjgl3", true);
                NativeLibraryLoader.loadNativeLibrary("jinput", true);
                NativeLibraryLoader.loadNativeLibrary("jinput-dx8", true);

                JmeContextOffscreenSurface.setLocalStage(stage);
                try {
                    super.start();
                } finally {
                    JmeContextOffscreenSurface.setLocalStage(null);
                }
            }

            @Override
            public void simpleInitApp() {
                // to prevent a NPE (due to setUseInput(null)) on Application.stop()
                getStateManager().detach(getStateManager().getState(DebugKeysAppState.class));
                /** this blue box is our player character */
                Box b = new Box(1, 1, 1);
                player = new Geometry("blue cube", b);
                Material mat = new Material(assetManager,
                        "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", ColorRGBA.Blue);
                player.setMaterial(mat);
                rootNode.attachChild(player);
            }

            /* Use the main event loop to trigger repeating actions. */
            @Override
            public void simpleUpdate(float tpf) {
                // make the player rotate:
                player.rotate(0, 2*tpf, 0);
            }
        };
        app.setSettings(settings);
        app.setShowSettings(false);
        return app;
    }
}
