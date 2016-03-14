/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx;

import com.jme3.input.RawInputListener;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;

import org.lwjgl.opengl.Display;

import java.awt.event.KeyEvent;
import java.util.BitSet;

import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * Converts JMEEvents to JFXEvents
 *
 * @author Heist
 */
public class JmeFXInputListener implements RawInputListener {

    /**
     * Контейнер Java FX.
     */
    private final JmeFxContainer jmeFxContainer;

    private final BitSet keyStateSet = new BitSet(0xFF);

    /**
     * Набор массивов для каждого символа.
     */
    private final char[][] keyCharArray = new char[Character.MAX_CODE_POINT][];

    /**
     * Таблица символов.
     */
    private final char[] keyCharSet = new char[Character.MAX_CODE_POINT];

    /**
     * Состояние кнопок мыши.
     */
    private final boolean[] mouseButtonState = new boolean[3];

    /**
     * Слушатель ввода пользователя.
     */
    private volatile RawInputListener everListeningInputListenerAdapter;

    /**
     * Обработчик DnD Java FX.
     */
    private volatile JmeFxDNDHandler jfxdndHandler;

    public JmeFXInputListener(final JmeFxContainer listensOnContainer) {
        this.jmeFxContainer = listensOnContainer;

        for (int i = 0, length = keyCharArray.length; i < length; i++) {
            keyCharArray[i] = new char[]{(char) i};
        }
    }

    /**
     * @return обработчик DnD Java FX.
     */
    private JmeFxDNDHandler getJfxdndHandler() {
        return jfxdndHandler;
    }

    @Override
    public void beginInput() {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.beginInput();
        }
    }

    @Override
    public void endInput() {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.endInput();
        }
    }

    /**
     * @return слушатель ввода пользователя.
     */
    private RawInputListener getEverListeningInputListenerAdapter() {
        return everListeningInputListenerAdapter;
    }

    /**
     * @return контейнер Java FX.
     */
    private JmeFxContainer getJmeFxContainer() {
        return jmeFxContainer;
    }

    /**
     * @return набор массивов для каждого символа.
     */
    private char[][] getKeyCharArray() {
        return keyCharArray;
    }

    /**
     * @return таблица символов.
     */
    private char[] getKeyCharSet() {
        return keyCharSet;
    }

    /**
     * @return
     */
    private BitSet getKeyStateSet() {
        return keyStateSet;
    }

    /**
     * @return состояние кнопок мыши.
     */
    private boolean[] getMouseButtonState() {
        return mouseButtonState;
    }

    @Override
    public void onJoyAxisEvent(final JoyAxisEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onJoyAxisEvent(event);
        }
    }

    @Override
    public void onJoyButtonEvent(final JoyButtonEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onJoyButtonEvent(event);
        }
    }

    @Override
    public void onKeyEvent(final KeyInputEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onKeyEvent(event);
        }

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final EmbeddedSceneInterface scenePeer = jmeFxContainer.getScenePeer();

        if (scenePeer == null) {
            return;
        }

        final BitSet keyStateSet = getKeyStateSet();

        final char[][] keyCharArray = getKeyCharArray();
        final char[] keyCharSet = getKeyCharSet();
        final char keyChar = event.getKeyChar();

        int fxKeyCode = AwtKeyInput.convertJmeCode(event.getKeyCode());

        final int keyState = retrieveKeyState();

        if (fxKeyCode > keyCharSet.length) {
            switch (keyChar) {
                case '\\': {
                    fxKeyCode = java.awt.event.KeyEvent.VK_BACK_SLASH;
                    break;
                }
                default: {
                    return;
                }
            }
        }

        if (jmeFxContainer.isFocus()) {
            event.setConsumed();
        }

        if (event.isRepeating()) {

            final char x = keyCharSet[fxKeyCode];

            if (jmeFxContainer.isFocus()) {
                scenePeer.keyEvent(AbstractEvents.KEYEVENT_TYPED, fxKeyCode, keyCharArray[x], keyState);
            }

        } else if (event.isPressed()) {

            keyCharSet[fxKeyCode] = keyChar;
            keyStateSet.set(fxKeyCode);

            if (jmeFxContainer.isFocus()) {
                scenePeer.keyEvent(AbstractEvents.KEYEVENT_PRESSED, fxKeyCode, keyCharArray[keyChar], keyState);
                scenePeer.keyEvent(AbstractEvents.KEYEVENT_TYPED, fxKeyCode, keyCharArray[keyChar], keyState);
            }

        } else {

            final char x = keyCharSet[fxKeyCode];

            keyStateSet.clear(fxKeyCode);

            if (jmeFxContainer.isFocus()) {
                scenePeer.keyEvent(AbstractEvents.KEYEVENT_RELEASED, fxKeyCode, keyCharArray[x], keyState);
            }
        }
    }

    @Override
    public void onMouseButtonEvent(final MouseButtonEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onMouseButtonEvent(event);
        }

        // TODO: Process events in separate thread ?
        final JmeFxContainer jmeFxContainer = getJmeFxContainer();

        if (!jmeFxContainer.isVisibleCursor() || jmeFxContainer.getScenePeer() == null) {
            return;
        }

        final Scene scene = jmeFxContainer.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        int button;

        switch (event.getButtonIndex()) {
            case 0: {
                button = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                break;
            }
            case 1: {
                button = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                break;
            }
            case 2: {
                button = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                break;
            }
            default: {
                return;
            }
        }

        mouseButtonState[event.getButtonIndex()] = event.isPressed();

        // seems that generating mouse release without corresponding mouse
        // pressed is causing problems in Scene.ClickGenerator

        final boolean covered = jmeFxContainer.isCovered(x, y);

        if (!covered) {
            jmeFxContainer.loseFocus();
        } else {
            event.setConsumed();
            jmeFxContainer.grabFocus();
        }

        int type;
        if (event.isPressed()) {
            type = AbstractEvents.MOUSEEVENT_PRESSED;
        } else if (event.isReleased()) {
            type = AbstractEvents.MOUSEEVENT_RELEASED;
            // and clicked ??
        } else {
            return;
        }

        Platform.runLater(() -> {

            final boolean[] mouseButtonState = getMouseButtonState();
            final JmeFxDNDHandler jfxdndHandler = getJfxdndHandler();

            if (jfxdndHandler != null) {
                jfxdndHandler.mouseUpdate(x, y, mouseButtonState[0]);
            }

            final JmeFxContainer fxContainer = getJmeFxContainer();
            final EmbeddedSceneInterface scenePeer = fxContainer.getScenePeer();

            scenePeer.mouseEvent(type, button, mouseButtonState[0], mouseButtonState[1], mouseButtonState[2], x, y, fxContainer.getOldX() + x, fxContainer.getOldY() + y, keyStateSet.get(KeyEvent.VK_SHIFT), keyStateSet.get(KeyEvent.VK_CONTROL), keyStateSet.get(KeyEvent.VK_ALT), keyStateSet.get(KeyEvent.VK_META), 0, button == AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON);
        });
    }

    @Override
    public void onMouseMotionEvent(final MouseMotionEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onMouseMotionEvent(event);
        }

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();

        if (!jmeFxContainer.isVisibleCursor() || jmeFxContainer.getScenePeer() == null) {
            return;
        }

        final Scene scene = jmeFxContainer.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        final boolean covered = jmeFxContainer.isCovered(x, y);

        if (covered) {
            event.setConsumed();
        }

        // not sure if should be grabbing focus on mouse motion event
        // grabFocus();

        int type = AbstractEvents.MOUSEEVENT_MOVED;
        int button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;

        final int wheelRotation = (int) Math.round(event.getDeltaWheel() / -120.0);

        final boolean[] mouseButtonState = getMouseButtonState();

        if (wheelRotation != 0) {
            type = AbstractEvents.MOUSEEVENT_WHEEL;
            button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;
        } else if (mouseButtonState[0]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
        } else if (mouseButtonState[1]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
        } else if (mouseButtonState[2]) {
            type = AbstractEvents.MOUSEEVENT_DRAGGED;
            button = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
        }

        final int ftype = type;
        final int fbutton = button;

        /**
         * ensure drag and drop is handled before the mouse release event fires
         */
        Platform.runLater(() -> {
            if (JmeFXInputListener.this.jfxdndHandler != null) {
                JmeFXInputListener.this.jfxdndHandler.mouseUpdate(x, y, JmeFXInputListener.this.mouseButtonState[0]);
            }
            JmeFXInputListener.this.jmeFxContainer.scenePeer.mouseEvent(ftype, fbutton, JmeFXInputListener.this.mouseButtonState[0], JmeFXInputListener.this.mouseButtonState[1], JmeFXInputListener.this.mouseButtonState[2], x, y, Display.getX() + x, Display.getY() + y, JmeFXInputListener.this.keyStateSet.get(KeyEvent.VK_SHIFT), JmeFXInputListener.this.keyStateSet.get(KeyEvent.VK_CONTROL), JmeFXInputListener.this.keyStateSet.get(KeyEvent.VK_ALT), JmeFXInputListener.this.keyStateSet.get(KeyEvent.VK_META), wheelRotation, false);
        });
    }

    @Override
    public void onTouchEvent(final TouchEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();

        if (adapter != null) {
            adapter.onTouchEvent(event);
        }
    }

    public int retrieveKeyState() {

        int embedModifiers = 0;

        if (this.keyStateSet.get(KeyEvent.VK_SHIFT)) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }

        if (this.keyStateSet.get(KeyEvent.VK_CONTROL)) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }

        if (this.keyStateSet.get(KeyEvent.VK_ALT)) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }

        if (this.keyStateSet.get(KeyEvent.VK_META)) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }

        return embedModifiers;
    }

    public void setEverListeningRawInputListener(final RawInputListener rawInputListenerAdapter) {
        this.everListeningInputListenerAdapter = rawInputListenerAdapter;
    }

    /**
     * set on drag start /nulled on end<br>
     * necessary so that the drag events can be generated appropiatly
     *
     * @param jfxdndHandler
     */
    public void setMouseDNDListener(final JmeFxDNDHandler jfxdndHandler) {
        assert this.jfxdndHandler == null || jfxdndHandler == null : "duplicate jfxdndn handler register? ";
        this.jfxdndHandler = jfxdndHandler;
    }
}
