package com.jme3x.jfx.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Набор утильных методов для работы с Pixels в JavaFX.
 */
public class JFXPixels {

    public static final int BYTE_BGRA_PRE;
    public static final int BYTE_ARGB;

    private static final Class<?> PIXELS_TYPE;
    private static final Class<?> FORMAT_TYPE;

    private static final String FIELD_BYTE_BGRA_PRE = "BYTE_BGRA_PRE";
    private static final String FIELD_BYTE_ARGB = "BYTE_ARGB";

    private static final String METHOD_GET_NATIVE_FORMAT = "getNativeFormat";

    private static final MethodHandle GET_NATIVE_FORMAT_HANDLE;

    static {

        try {

            PIXELS_TYPE = Class.forName("com.sun.glass.ui.Pixels");
            FORMAT_TYPE = PIXELS_TYPE.getDeclaredClasses()[0];

            final Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);

            final MethodHandles.Lookup pixelsLookup = lookupConstructor.newInstance(PIXELS_TYPE);
            GET_NATIVE_FORMAT_HANDLE = pixelsLookup.findStatic(PIXELS_TYPE, METHOD_GET_NATIVE_FORMAT, MethodType.methodType(int.class));

            final MethodHandles.Lookup formatLookup = lookupConstructor.newInstance(FORMAT_TYPE);
            final VarHandle bgraVarField = formatLookup.findStaticVarHandle(FORMAT_TYPE, FIELD_BYTE_BGRA_PRE, int.class);
            final VarHandle argbVarField = formatLookup.findStaticVarHandle(FORMAT_TYPE, FIELD_BYTE_ARGB, int.class);

            BYTE_BGRA_PRE = (Integer) bgraVarField.get();
            BYTE_ARGB = (Integer) argbVarField.get();

        } catch (final ClassNotFoundException | NoSuchFieldException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Получение формата изображения.
     */
    public static int getNativeFormat() {
        try {
            return (Integer) GET_NATIVE_FORMAT_HANDLE.invoke();
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
