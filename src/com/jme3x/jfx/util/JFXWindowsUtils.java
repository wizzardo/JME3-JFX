package com.jme3x.jfx.util;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3x.jfx.util.os.OperatingSystem;

import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static java.lang.ThreadLocal.withInitial;
import static org.lwjgl.BufferUtils.createIntBuffer;

/**
 * Набор методов по работе с окном.
 *
 * @author Ronn
 */
public class JFXWindowsUtils {

    private static final Map<String, Point> OFFSET_MAPPING = new HashMap<>();

    static {
        OFFSET_MAPPING.put("Ubuntu", new Point(0, 0));
    }

    private static final ThreadLocal<IntBuffer> LOCAL_FIRST_INT_BUFFER = withInitial(() -> createIntBuffer(1));
    private static final ThreadLocal<IntBuffer> LOCAL_SECOND_INT_BUFFER = withInitial(() -> createIntBuffer(1));

    /**
     * Получение размера декарации окна.
     */
    public static Point getWindowDecorationSize() {

        final OperatingSystem system = new OperatingSystem();
        final String distribution = system.getDistribution();

        if (OFFSET_MAPPING.containsKey(distribution)) {
            return OFFSET_MAPPING.get(distribution);
        }

        for (final Map.Entry<String, Point> entry : OFFSET_MAPPING.entrySet()) {
            final String key = entry.getKey();
            if (distribution.startsWith(key)) {
                return entry.getValue();
            }
        }

        return new Point(0, 0);
    }

    /**
     * Получение текущей X координаты окна.
     */
    public static int getX(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer x = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer y = LOCAL_SECOND_INT_BUFFER.get();
        x.clear();
        y.clear();

        GLFW.glfwGetWindowPos(windowHandle, x, y);

        return x.get(0);
    }

    /**
     * Получение текущей Y координаты окна.
     */
    public static int getY(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer x = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer y = LOCAL_SECOND_INT_BUFFER.get();
        x.clear();
        y.clear();

        GLFW.glfwGetWindowPos(windowHandle, x, y);

        return y.get(0);
    }

    /**
     * Получение текущей ширины окна.
     */
    public static int getWidth(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer width = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer height = LOCAL_SECOND_INT_BUFFER.get();
        width.clear();
        height.clear();

        GLFW.glfwGetWindowSize(windowHandle, width, height);

        return width.get(0);
    }

    /**
     * Получение текущей высоты окна.
     */
    public static int getHeight(final JmeContext context) {

        final LwjglWindow lwjglContext = (LwjglWindow) context;
        final long windowHandle = lwjglContext.getWindowHandle();

        final IntBuffer width = LOCAL_FIRST_INT_BUFFER.get();
        final IntBuffer height = LOCAL_SECOND_INT_BUFFER.get();
        width.clear();
        height.clear();

        GLFW.glfwGetWindowSize(windowHandle, width, height);

        return height.get(0);
    }

    /**
     * Проверка находится ли окно в полноэкранном режиме.
     */
    public static boolean isFullscreen(final JmeContext jmeContext) {
        final AppSettings settings = jmeContext.getSettings();
        return settings.isFullscreen();
    }

    /**
     * Запросить фокус на окне.
     */
    public static void requestFocus(final Application application) {
        final LwjglWindow lwjglContext = (LwjglWindow) application.getContext();
        GLFW.glfwShowWindow(lwjglContext.getWindowHandle());
    }
}
