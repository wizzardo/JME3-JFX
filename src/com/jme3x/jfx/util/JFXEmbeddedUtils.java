package com.jme3x.jfx.util;

import com.jme3x.jfx.JmeJFXPanel;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.IntBuffer;

import javafx.embed.swing.JFXPanel;
import rlib.util.array.ArrayFactory;

/**
 * Набор утильных методов по работе со встроенным окном JavaFX.
 *
 * @author Ronn
 */
public class JFXEmbeddedUtils {

    private static final Class<?> STAGE_TYPE;
    private static final Class<?> SCENE_TYPE;

    private static final String FIELD_STAGE_PEER = "stagePeer";
    private static final String FIELD_SCENE_PEER = "scenePeer";

    private static final String FIELD_P_WIDTH = "pWidth";
    private static final String FIELD_P_HEIGHT = "pHeight";

    private static final String FIELD_SCENE_X = "screenX";
    private static final String FIELD_SCENE_Y = "screenY";

    private static final String FIELD_IS_CAPTURING_MOUSE = "isCapturingMouse";

    private static final String METHOD_GET_PIXELS = "getPixels";
    private static final String METHOD_SET_PIXEL_SCALE_FACTORS = "setPixelScaleFactors";

    private static final String METHOD_SEND_RESIZE_EVENT_TO_FX = "sendResizeEventToFX";
    private static final String METHOD_SEND_MOVE_EVENT_TO_FX = "sendMoveEventToFX";
    private static final String METHOD_SEND_MOUSE_EVENT_TO_FX = "sendMouseEventToFX";
    private static final String METHOD_SEND_FOCUS_EVENT_TO_FX = "sendFocusEventToFX";
    private static final String METHOD_SEND_KEY_EVENT_TO_FX = "sendKeyEventToFX";

    private static final VarHandle STAGE_VAR_HANDLE;
    private static final VarHandle SCENE_VAR_HANDLE;

    private static final VarHandle P_WIDTH_VAR_HANDLE;
    private static final VarHandle P_HEIGHT_VAR_HANDLE;

    private static final VarHandle SCREEN_X_VAR_HANDLE;
    private static final VarHandle SCREEN_Y_VAR_HANDLE;

    private static final VarHandle IS_CAPTURING_MOUSE_VAR_HANDLE;

    private static final MethodHandle SEND_RESIZE_EVENT_TO_FX_HANDLE;
    private final static MethodHandle SEND_MOVE_EVENT_TO_FX_HANDLE;
    private final static MethodHandle SEND_MOUSE_EVENT_TO_FX_HANDLE;
    private final static MethodHandle SEND_FOCUS_EVENT_TO_FX_HANDLE;
    private final static MethodHandle SEND_KEY_EVENT_TO_FX_HANDLE;

    private final static MethodHandle GET_PIXELS_HANDLE;
    private final static MethodHandle SET_PIXEL_SCALE_FACTORS_HANDLE;

    static {

        try {

            STAGE_TYPE = Class.forName("com.sun.javafx.embed.EmbeddedStageInterface");
            SCENE_TYPE = Class.forName("com.sun.javafx.embed.EmbeddedSceneInterface");

            final Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            lookupConstructor.setAccessible(true);

            final MethodHandles.Lookup panelLookup = lookupConstructor.newInstance(JFXPanel.class);
            STAGE_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_STAGE_PEER, STAGE_TYPE);
            SCENE_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_SCENE_PEER, SCENE_TYPE);

            P_WIDTH_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_P_WIDTH, int.class);
            P_HEIGHT_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_P_HEIGHT, int.class);

            SCREEN_X_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_SCENE_X, int.class);
            SCREEN_Y_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_SCENE_Y, int.class);

            IS_CAPTURING_MOUSE_VAR_HANDLE = panelLookup.findVarHandle(JFXPanel.class, FIELD_IS_CAPTURING_MOUSE, boolean.class);

            SEND_RESIZE_EVENT_TO_FX_HANDLE = panelLookup.findVirtual(JFXPanel.class, METHOD_SEND_RESIZE_EVENT_TO_FX, MethodType.methodType(void.class));
            SEND_MOVE_EVENT_TO_FX_HANDLE = panelLookup.findVirtual(JFXPanel.class, METHOD_SEND_MOVE_EVENT_TO_FX, MethodType.methodType(void.class));
            SEND_MOUSE_EVENT_TO_FX_HANDLE = panelLookup.findVirtual(JFXPanel.class, METHOD_SEND_MOUSE_EVENT_TO_FX, MethodType.methodType(void.class, MouseEvent.class));
            SEND_FOCUS_EVENT_TO_FX_HANDLE = panelLookup.findVirtual(JFXPanel.class, METHOD_SEND_FOCUS_EVENT_TO_FX, MethodType.methodType(void.class, FocusEvent.class));
            SEND_KEY_EVENT_TO_FX_HANDLE = panelLookup.findVirtual(JFXPanel.class, METHOD_SEND_KEY_EVENT_TO_FX, MethodType.methodType(void.class, KeyEvent.class));

            final MethodType getPixelsMethodType = MethodType.methodType(boolean.class, ArrayFactory.toArray(IntBuffer.class, int.class, int.class));
            final MethodType setPixelScaleFactorsMethodType = MethodType.methodType(void.class, ArrayFactory.toArray(float.class, float.class));

            final MethodHandles.Lookup sceneLookup = lookupConstructor.newInstance(SCENE_TYPE);
            GET_PIXELS_HANDLE = sceneLookup.findVirtual(SCENE_TYPE, METHOD_GET_PIXELS, getPixelsMethodType);
            SET_PIXEL_SCALE_FACTORS_HANDLE = sceneLookup.findVirtual(SCENE_TYPE, METHOD_SET_PIXEL_SCALE_FACTORS, setPixelScaleFactorsMethodType);

        } catch (final NoSuchFieldException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Вытащить из панели EmbeddedStage.
     */
    public static Object getStage(final JmeJFXPanel panel) {
        return STAGE_VAR_HANDLE.get(panel);
    }

    /**
     * Вытащить из панели EmbeddedScene.
     */
    public static Object getScene(final JmeJFXPanel panel) {
        return SCENE_VAR_HANDLE.get(panel);
    }

    /**
     * Установка новой высоты для EmbeddedScene.
     */
    public static void setPHeight(final JmeJFXPanel panel, final int value) {
        P_HEIGHT_VAR_HANDLE.set(panel, value);
    }

    /**
     * Установка новой высоты для EmbeddedStage.
     */
    public static void setPWidth(final JmeJFXPanel panel, final int value) {
        P_WIDTH_VAR_HANDLE.set(panel, value);
    }

    /**
     * Установка новой позиции по X для EmbeddedStage.
     */
    public static void setScreenX(final JmeJFXPanel panel, final int value) {
        SCREEN_X_VAR_HANDLE.getAndSet(panel, value);
    }

    /**
     * Установка новой позиции по Y для EmbeddedStage.
     */
    public static void setScreenY(final JmeJFXPanel panel, final int value) {
        SCREEN_Y_VAR_HANDLE.getAndSet(panel, value);
    }

    /**
     * Установить флаг захвата движения мышкой.
     */
    public static void setCapturingMouse(final JmeJFXPanel panel, final boolean value) {
        IS_CAPTURING_MOUSE_VAR_HANDLE.set(panel, value);
    }

    /**
     * Обновить положение EmbeddedScene.
     */
    public static void sendMoveEventToFX(final JmeJFXPanel panel) {
        try {
            SEND_MOVE_EVENT_TO_FX_HANDLE.invoke(panel);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Обновить размер EmbeddedScene.
     */
    public static void sendResizeEventToFX(final JmeJFXPanel panel) {
        try {
            SEND_RESIZE_EVENT_TO_FX_HANDLE.invoke(panel);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправить на обработку событие связанное с мышью в EmbeddedScene.
     */
    public static void sendMouseEventToFX(final JmeJFXPanel panel, final MouseEvent event) {
        try {
            SEND_MOUSE_EVENT_TO_FX_HANDLE.invoke(panel, event);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправить на обработку событие связанное с фокусом окна в EmbeddedScene.
     */
    public static void sendFocusEventToFX(final JmeJFXPanel panel, final FocusEvent event) {
        try {
            SEND_FOCUS_EVENT_TO_FX_HANDLE.invoke(panel, event);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Отправить на обработку событие связанное с клавиатурой в EmbeddedScene.
     */
    public static void sendKeyEventToFX(final JmeJFXPanel panel, final KeyEvent event) {
        try {
            SEND_KEY_EVENT_TO_FX_HANDLE.invoke(panel, event);
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Запрос на получение данных об отрисованном UI.
     *
     * @param panel  панель из которой надо изъять данные.
     * @param buffer буффер для размещения данных.
     * @param width  ширина.
     * @param height высота.
     * @return были ли данные получены.
     */
    public static boolean getPixels(final JmeJFXPanel panel, final IntBuffer buffer, final int width, final int height) {

        final Object embeddedScene = panel.getEmbeddedScene();
        final Object result;
        try {
            result = GET_PIXELS_HANDLE.invokeWithArguments(embeddedScene, buffer, width, height);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        return (boolean) result;
    }

    /**
     * Установка маштабирования пикселей в сцене.
     *
     * @param panel  панель в которй находится сцена.
     * @param scaleX маштабирование по оси X.
     * @param scaleY маштабирование по оси Y.
     */
    public static void setPixelScaleFactors(final JmeJFXPanel panel, final float scaleX, final float scaleY) {

        final Object embeddedScene = panel.getEmbeddedScene();
        try {
            SET_PIXEL_SCALE_FACTORS_HANDLE.invokeWithArguments(embeddedScene, scaleX, scaleY);
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
