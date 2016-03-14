package com.jme3x.jfx.util;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.lwjgl.LwjglWindow;
import com.jme3x.jfx.util.os.OperatingSystem;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Set of methods for scrap work JFX.
 *
 * @author Ronn
 */
public class JFXUtils {

    public static final String PROP_DISPLAY_UNDECORATED = "org.lwjgl.opengl.Window.undecorated";

    private static final Map<String, Point> OFFSET_MAPPING = new HashMap<>();

    static {
       // OFFSET_MAPPING.put("Ubuntu 14.04 LTS (trusty)", new Point(10, 37));
       // OFFSET_MAPPING.put("Ubuntu 14.04.1 LTS (trusty)", new Point(10, 37));
       // OFFSET_MAPPING.put("Ubuntu 14.04.2 LTS (trusty)", new Point(0, 26));
        //OFFSET_MAPPING.put("Ubuntu 14.04", new Point(0, 26));
    }

    private static final ThreadLocal<IntBuffer> LOCAL_FIRST_INT_BUFFER = new ThreadLocal<IntBuffer>() {

        @Override
        protected IntBuffer initialValue() {
            return BufferUtils.createIntBuffer(1);
        }
    };

    private static final ThreadLocal<IntBuffer> LOCAL_SECOND_INT_BUFFER = new ThreadLocal<IntBuffer>() {

        @Override
        protected IntBuffer initialValue() {
            return BufferUtils.createIntBuffer(1);
        }
    };

    /**
     * Getting the size of the window decorations in the system.
     */
    public static final Point getWindowDecorationSize() {

        if ("true".equalsIgnoreCase(System.getProperty(PROP_DISPLAY_UNDECORATED))) {
            return new Point(0, 0);
        }

        final OperatingSystem system = new OperatingSystem();
        final String distribution = system.getDistribution();

        if (OFFSET_MAPPING.containsKey(distribution)) {
            return OFFSET_MAPPING.get(distribution);
        }

        for (final Map.Entry<String, Point> entry : OFFSET_MAPPING.entrySet()) {

            final String key = entry.getKey();

            if(distribution.startsWith(key)) {
                return entry.getValue();
            }
        }

        return new Point(0, 25);
    }

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

    public static boolean isFullscreen(final JmeContext jmeContext) {
        final AppSettings settings = jmeContext.getSettings();
        return settings.isFullscreen();
    }
}
