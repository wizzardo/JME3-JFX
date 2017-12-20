package com.jme3x.jfx.util;

import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * The class with additional utility methods for JavaFX Platform.
 *
 * @author JavaSaBr
 */
public class JFXPlatform {

    /**
     * Execute the task in JavaFX thread.
     *
     * @param task the task.
     */
    public static void runInFXThread(@NotNull final Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
