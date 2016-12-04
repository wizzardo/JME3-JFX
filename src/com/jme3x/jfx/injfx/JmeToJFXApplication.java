package com.jme3x.jfx.injfx;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;

import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import static com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext.setLocalStage;

/**
 * The base implementation of {@link Application} for using in the {@link ImageView}.
 *
 * @author JavaSaBr.
 */
public class JmeToJFXApplication extends SimpleApplication {

    private static final ApplicationThreadExecutor EXECUTOR = ApplicationThreadExecutor.getInstance();

    private final Stage stage;

    public JmeToJFXApplication(final Stage stage) {
        this.stage = stage;
    }

    @Override
    public void start() {
        setLocalStage(stage);
        try {
            super.start();
        } finally {
            setLocalStage(null);
        }
    }

    @Override
    public void update() {
        EXECUTOR.execute();
        super.update();
    }

    @Override
    public void simpleInitApp() {
    }
}
