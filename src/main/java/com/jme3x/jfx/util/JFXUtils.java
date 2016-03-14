package com.jme3x.jfx.util;

import com.jme3x.jfx.util.os.OperatingSystem;

import java.awt.*;
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
        OFFSET_MAPPING.put("Ubuntu 14.04 LTS (trusty)", new Point(10, 37));
        OFFSET_MAPPING.put("Ubuntu 14.04.1 LTS (trusty)", new Point(10, 37));
        OFFSET_MAPPING.put("Ubuntu 14.04.2 LTS (trusty)", new Point(0, 26));
        OFFSET_MAPPING.put("Ubuntu 14.04", new Point(0, 26));
    }

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

        return new Point(3, 25);
    }
}
