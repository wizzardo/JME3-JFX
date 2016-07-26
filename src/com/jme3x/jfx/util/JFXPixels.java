package com.jme3x.jfx.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javafx.application.Platform;
import rlib.util.array.ArrayFactory;

/**
 * Набор утильных методов для работы с Pixels в JavaFX.
 */
public class JFXPixels {

    private static final Class<?> PIXELS_TYPE;
    private static final Class<?> FORMAT_TYPE;

    private static final String METHOD_STARTUP = "startup";

    private static final MethodHandle STARTUP_HANDLE;

    static {

        try {

            PIXELS_TYPE = Class.forName("com.sun.glass.ui.Pixels");
            FORMAT_TYPE = Class.forName("com.sun.glass.ui.Pixels.Format");

            final Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);

            final MethodHandles.Lookup pixelsLookup = lookupConstructor.newInstance(PIXELS_TYPE);
            STARTUP_HANDLE = pixelsLookup.findStatic(PIXELS_TYPE, METHOD_STARTUP, MethodType.methodType(Void.class, ArrayFactory.toArray(Runnable.class)));

        } catch (final ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Добавить задачу на выполнение после старта JavaFX.
     */
    public static void startup(final Runnable task) {
        try {
            STARTUP_HANDLE.invokeWithArguments(task);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Выполнить задачу в потоке JavaFX.
     */
    public static void runInFXThread(final Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }
}
