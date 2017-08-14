package com.jme3x.jfx.injfx;

import static com.ss.rlib.util.ObjectUtils.notNull;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.post.FilterPostProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The base implementation of {@link Application} for using in the JavaFX.
 *
 * @author JavaSaBr.
 */
public class JmeToJFXApplication extends SimpleApplication {

    @NotNull
    private static final ApplicationThreadExecutor EXECUTOR = ApplicationThreadExecutor.getInstance();

    /**
     * The post filter processor.
     */
    @Nullable
    protected FilterPostProcessor postProcessor;

    /**
     * Instantiates a new Jme to jfx application.
     */
    public JmeToJFXApplication() {
    }

    @Override
    public void update() {
        EXECUTOR.execute();
        super.update();
    }

    @Override
    public void simpleInitApp() {
        postProcessor = new FilterPostProcessor(assetManager);
        postProcessor.initialize(renderManager, viewPort);
        viewPort.addProcessor(postProcessor);
    }

    /**
     * Get the post filter processor.
     *
     * @return the post filter processor.
     */
    @NotNull
    protected FilterPostProcessor getPostProcessor() {
        return notNull(postProcessor);
    }
}
