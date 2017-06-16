package com.jme3x.jfx.util;

import com.ss.rlib.util.array.ArrayFactory;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The class with additional utility methods for JavaFX Platform.
 *
 * @author JavaSaBr.
 */
public class JFXPlatform {

    private static final Class<?> PLATFORM_TYPE;

    private static final String METHOD_STARTUP = "startup";

    /**
     * The example of getting a method handle.
     */
    @NotNull
    private static final MethodHandle STARTUP_HANDLE;

    static {

        try {

            PLATFORM_TYPE = Class.forName("com.sun.javafx.application.PlatformImpl");

            final Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);

            final MethodHandles.Lookup platformLookup = lookupConstructor.newInstance(PLATFORM_TYPE);
            STARTUP_HANDLE = platformLookup.findStatic(PLATFORM_TYPE, METHOD_STARTUP, MethodType.methodType(void.class, ArrayFactory.toArray(Runnable.class)));

        } catch (final ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Execute the task in JavaFX thread.
     *
     * @param task the task
     */
    public static void runInFXThread(@NotNull final Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
