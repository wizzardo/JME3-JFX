package com.jme3x.jfx.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javafx.scene.input.TransferMode;

import static rlib.util.array.ArrayFactory.toArray;

/**
 * Набор утильных методов для работы с EmbeddedDND в JavaFX.
 */
public class JFXDNDUtils {

    public static final Class<?> DRAG_SOURCE_TYPE;
    public static final Class<?> DRAG_TARGET_TYPE;

    private static final String METHOD_HANDLE_DRAG_OVER = "handleDragOver";
    private static final String METHOD_HANDLE_DRAG_DROP = "handleDragDrop";
    private static final String METHOD_HANDLE_DRAG_LEAVE = "handleDragLeave";
    private static final String METHOD_HANDLE_DRAG_ENTER = "handleDragEnter";

    private static final String METHOD_DRAG_DROP_END = "dragDropEnd";
    private static final String METHOD_GET_DATA = "getData";

    private static final MethodHandle DRAG_DROP_END_HANDLE;
    private static final MethodHandle GET_DATA_HANDLE;

    private static final MethodHandle HANDLE_DRAG_OVER_HANDLE;
    private static final MethodHandle HANDLE_DRAG_DROP_HANDLE;
    private static final MethodHandle HANDLE_DRAG_LEAVE_HANDLE;
    private static final MethodHandle HANDLE_DRAG_ENTER_HANDLE;

    static {

        try {

            DRAG_SOURCE_TYPE = Class.forName("com.sun.javafx.embed.EmbeddedSceneDSInterface");
            DRAG_TARGET_TYPE = Class.forName("com.sun.javafx.embed.EmbeddedSceneDTInterface");

            final Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);

            final MethodHandles.Lookup sourceLookup = lookupConstructor.newInstance(DRAG_SOURCE_TYPE);

            DRAG_DROP_END_HANDLE = sourceLookup.findVirtual(DRAG_SOURCE_TYPE, METHOD_DRAG_DROP_END, MethodType.methodType(void.class, TransferMode.class));
            GET_DATA_HANDLE = sourceLookup.findVirtual(DRAG_SOURCE_TYPE, METHOD_GET_DATA, MethodType.methodType(Object.class, String.class));

            final MethodHandles.Lookup targetLookup = lookupConstructor.newInstance(DRAG_TARGET_TYPE);
            final MethodType handleDragOverMethodType = MethodType.methodType(TransferMode.class, toArray(int.class, int.class, int.class, int.class, TransferMode.class));
            final MethodType handleDragDropMethodType = MethodType.methodType(TransferMode.class, toArray(int.class, int.class, int.class, int.class, TransferMode.class));
            final MethodType handleDragEnterMethodType = MethodType.methodType(TransferMode.class, toArray(int.class, int.class, int.class, int.class, TransferMode.class, DRAG_SOURCE_TYPE));

            HANDLE_DRAG_LEAVE_HANDLE = targetLookup.findVirtual(DRAG_TARGET_TYPE, METHOD_HANDLE_DRAG_LEAVE, MethodType.methodType(void.class));
            HANDLE_DRAG_OVER_HANDLE = targetLookup.findVirtual(DRAG_TARGET_TYPE, METHOD_HANDLE_DRAG_OVER, handleDragOverMethodType);
            HANDLE_DRAG_DROP_HANDLE = targetLookup.findVirtual(DRAG_TARGET_TYPE, METHOD_HANDLE_DRAG_DROP, handleDragDropMethodType);
            HANDLE_DRAG_ENTER_HANDLE = targetLookup.findVirtual(DRAG_TARGET_TYPE, METHOD_HANDLE_DRAG_ENTER, handleDragEnterMethodType);

        } catch (final ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static TransferMode handleDragEnter(final Object target, final int x, final int y, final TransferMode transferMode, final Object source) {
        try {
            return (TransferMode) HANDLE_DRAG_ENTER_HANDLE.invoke(target, x, y, x, y, transferMode, source);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static TransferMode handleDragOver(final Object target, final int x, final int y, final TransferMode transferMode) {
        try {
            return (TransferMode) HANDLE_DRAG_OVER_HANDLE.invoke(target, x, y, x, y, transferMode);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static TransferMode handleDragDrop(final Object target, final int x, final int y, final TransferMode transferMode) {
        try {
            return (TransferMode) HANDLE_DRAG_DROP_HANDLE.invoke(target, x, y, x, y, transferMode);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static Object getData(final Object source, final String name) {
        try {
            return GET_DATA_HANDLE.invoke(source, name);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static void handleDragLeave(final Object target) {
        try {
            HANDLE_DRAG_LEAVE_HANDLE.invoke(target);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static void dragDropEnd(final Object source, final TransferMode transferMode) {
        try {
            DRAG_DROP_END_HANDLE.invoke(source, transferMode);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
