package com.jme3x.jfx.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javafx.application.Platform;
import rlib.util.array.ArrayFactory;

/**
 * Набор утильных методов для работы с JavaFX.
 */
public class JFXPlatform {

    private static final Class<?> PLATFORM_TYPE;

    private static final String METHOD_STARTUP = "startup";

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
     * Добавить задачу на выполнение после старта JavaFX.
     */
    public static void startup(final Runnable task) {
        try {
            STARTUP_HANDLE.invoke(task);
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
