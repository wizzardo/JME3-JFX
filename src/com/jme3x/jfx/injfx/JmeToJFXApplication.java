package com.jme3x.jfx.injfx;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;

import javafx.scene.image.ImageView;

/**
 * The base implementation of {@link Application} for using in the {@link ImageView}.
 *
 * @author JavaSaBr.
 */
public class JmeToJFXApplication extends SimpleApplication {

    private static final ApplicationThreadExecutor EXECUTOR = ApplicationThreadExecutor.getInstance();

    public JmeToJFXApplication() {
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
