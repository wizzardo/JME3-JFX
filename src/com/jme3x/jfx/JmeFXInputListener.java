/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx;

import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.BitSet;

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

    /**
     * Набор зажатых кнопок.
     */
    private final BitSet keyStateSet = new BitSet(0xFF);

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
    //private volatile JmeFxDNDHandler jfxdndHandler;
    public JmeFXInputListener(final JmeFxContainer listensOnContainer) {
        this.jmeFxContainer = listensOnContainer;
    }

    /**
     * @return обработчик DnD Java FX.
     */
   /* private JmeFxDNDHandler getJfxdndHandler() {
        return jfxdndHandler;
    }*/
    @Override
    public void beginInput() {
        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.beginInput();
    }

    @Override
    public void endInput() {
        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.endInput();
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
     * @return таблица символов.
     */
    private char[] getKeyCharSet() {
        return keyCharSet;
    }

    /**
     * @return набор зажатых кнопок.
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
        if (adapter != null) adapter.onJoyAxisEvent(event);
    }

    @Override
    public void onJoyButtonEvent(final JoyButtonEvent event) {
        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.onJoyButtonEvent(event);
    }

    @Override
    public void onKeyEvent(final KeyInputEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.onKeyEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final JmeJFXPanel container = jmeFxContainer.getHostContainer();
        if (container == null) return;

        final BitSet keyStateSet = getKeyStateSet();
        final char[] keyCharSet = getKeyCharSet();
        final char keyChar = event.getKeyChar();

        final int keyCode = event.getKeyCode();

        int awtKeyCode = keyCode == KeyInput.KEY_UNKNOWN ? KeyEvent.VK_UNDEFINED : AwtKeyInput.convertJmeCode(keyCode);

        final int keyState = getKeyMask();

        if (awtKeyCode > keyCharSet.length) {
            switch (keyChar) {
                case '\\': {
                    awtKeyCode = KeyEvent.VK_BACK_SLASH;
                    break;
                }
                default: {
                    return;
                }
            }
        }

        if (jmeFxContainer.isFocus()) event.setConsumed();

        final long when = System.currentTimeMillis();

        if (event.isRepeating()) {

            final char x = keyCharSet[awtKeyCode];

            if (jmeFxContainer.isFocus()) {
                container.handleEvent(new KeyEvent(container, KeyEvent.KEY_TYPED, when, keyState, KeyEvent.VK_UNDEFINED, x));
            }

        } else if (event.isPressed()) {

            keyCharSet[awtKeyCode] = keyChar;
            keyStateSet.set(awtKeyCode);

            if (jmeFxContainer.isFocus()) {
                container.handleEvent(new KeyEvent(container, KeyEvent.KEY_PRESSED, when, keyState, awtKeyCode, keyChar));
                container.handleEvent(new KeyEvent(container, KeyEvent.KEY_TYPED, when, keyState, KeyEvent.VK_UNDEFINED, keyChar));
            }

        } else {

            final char x = keyCharSet[awtKeyCode];

            keyStateSet.clear(awtKeyCode);

            if (jmeFxContainer.isFocus()) {
                container.handleEvent(new KeyEvent(container, KeyEvent.KEY_RELEASED, when, keyState, awtKeyCode, x));
            }
        }
    }

    @Override
    public void onMouseButtonEvent(final MouseButtonEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.onMouseButtonEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final Application application = jmeFxContainer.getApplication();

        final InputManager inputManager = application.getInputManager();
        if (!inputManager.isCursorVisible()) return;

        final JmeJFXPanel container = jmeFxContainer.getHostContainer();
        if (container == null) return;

        final Scene scene = jmeFxContainer.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        int button;

        switch (event.getButtonIndex()) {
            case 0: {
                button = MouseEvent.BUTTON1;
                break;
            }
            case 1: {
                button = MouseEvent.BUTTON2;
                break;
            }
            case 2: {
                button = MouseEvent.BUTTON3;
                break;
            }
            default: {
                return;
            }
        }

        mouseButtonState[event.getButtonIndex()] = event.isPressed();

        final boolean covered = jmeFxContainer.isCovered(x, y);

        if (!covered) {
            jmeFxContainer.loseFocus();
        } else if (inputManager.isCursorVisible()) {
            event.setConsumed();
            jmeFxContainer.grabFocus();
        }

        int eventId;

        if (event.isPressed()) {
            eventId = MouseEvent.MOUSE_PRESSED;
        } else if (event.isReleased()) {
            eventId = MouseEvent.MOUSE_RELEASED;
        } else {
            return;
        }

        onMouseButtonEventImpl(x, y, button, eventId);
    }

    private void onMouseButtonEventImpl(final int x, final int y, final int button, final int eventId) {

        //final JmeFxDNDHandler jfxdndHandler = getJfxdndHandler();

        //if (jfxdndHandler != null) {
        //    jfxdndHandler.mouseUpdate(x, y, primaryBtnDown);
        //}

        final JmeFxContainer fxContainer = getJmeFxContainer();
        final JmeJFXPanel container = fxContainer.getHostContainer();

        final long when = System.currentTimeMillis();
        final int mask = getMouseMask();
        final boolean popupTrigger = button == MouseEvent.BUTTON2 && eventId == MouseEvent.MOUSE_PRESSED;

        container.handleEvent(new MouseEvent(container, eventId, when, mask, x, y, 1, popupTrigger, button));
    }

    private int getMouseMask() {

        final BitSet keyStateSet = getKeyStateSet();
        final boolean[] mouseButtonState = getMouseButtonState();

        int mask = 0;

        if (mouseButtonState[0]) {
            mask |= MouseEvent.BUTTON1_DOWN_MASK;
        }

        if (mouseButtonState[1]) {
            mask |= MouseEvent.BUTTON2_DOWN_MASK;
        }

        if (mouseButtonState[2]) {
            mask |= MouseEvent.BUTTON3_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
            mask |= MouseEvent.SHIFT_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_CONTROL)) {
            mask |= MouseEvent.CTRL_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_ALT)) {
            mask |= MouseEvent.ALT_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_META)) {
            mask |= MouseEvent.META_DOWN_MASK;
        }

        return mask;
    }

    @Override
    public void onMouseMotionEvent(final MouseMotionEvent event) {

        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.onMouseMotionEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final Application application = jmeFxContainer.getApplication();

        final InputManager inputManager = application.getInputManager();
        if (!inputManager.isCursorVisible()) return;

        final JmeJFXPanel container = jmeFxContainer.getHostContainer();
        if (container == null) return;

        final Scene scene = jmeFxContainer.getScene();

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        final boolean covered = jmeFxContainer.isCovered(x, y);
        if (covered) event.setConsumed();

        final boolean[] mouseButtonState = getMouseButtonState();

        int eventId = MouseEvent.MOUSE_DRAGGED;
        int button = MouseEvent.NOBUTTON;

        final int wheelRotation = (int) Math.round(event.getDeltaWheel() / -120.0);

        if (wheelRotation != 0) {
            eventId = MouseEvent.MOUSE_WHEEL;
        } else if (mouseButtonState[0]) {
            button = MouseEvent.BUTTON1;
        } else if (mouseButtonState[1]) {
            button = MouseEvent.BUTTON2;
        } else if (mouseButtonState[2]) {
            button = MouseEvent.BUTTON3;
        }

        onMouseMotionEventImpl(x, y, button, eventId, wheelRotation);
    }

    private void onMouseMotionEventImpl(final int x, final int y, final int button, final int eventId, final int wheelRotation) {

        final JmeFxContainer fxContainer = getJmeFxContainer();
        final JmeJFXPanel container = fxContainer.getHostContainer();

        /*
          final JmeFxDNDHandler dndHandler = getJfxdndHandler();

        if (dndHandler != null) dndHandler.mouseUpdate(x, y, primaryBtnDown);
         */

        final long when = System.currentTimeMillis();
        final int mask = getMouseMask();

        if (eventId == MouseEvent.MOUSE_WHEEL) {
            container.handleEvent(new MouseWheelEvent(container, eventId, when, mask, x, y, 1, false, button, MouseWheelEvent.WHEEL_UNIT_SCROLL, wheelRotation));
        } else {
            container.handleEvent(new MouseEvent(container, eventId, when, mask, x, y, 1, false, button));
        }
    }

    @Override
    public void onTouchEvent(final TouchEvent event) {
        final RawInputListener adapter = getEverListeningInputListenerAdapter();
        if (adapter != null) adapter.onTouchEvent(event);
    }

    private int getKeyMask() {

        int embedModifiers = 0;

        final BitSet keyStateSet = getKeyStateSet();

        if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
            embedModifiers |= KeyEvent.SHIFT_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_CONTROL)) {
            embedModifiers |= KeyEvent.CTRL_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_ALT)) {
            embedModifiers |= KeyEvent.ALT_DOWN_MASK;
        }

        if (keyStateSet.get(KeyEvent.VK_META)) {
            embedModifiers |= KeyEvent.META_DOWN_MASK;
        }

        return embedModifiers;
    }

    public void setEverListeningRawInputListener(final RawInputListener rawInputListenerAdapter) {
        this.everListeningInputListenerAdapter = rawInputListenerAdapter;
    }

    /**
     * set on drag start /nulled on end<br> necessary so that the drag events can be generated
     * appropiatly
     */
   /* public void setMouseDNDListener(final JmeFxDNDHandler jfxdndHandler) {
        assert this.jfxdndHandler == null || jfxdndHandler == null : "duplicate jfxdndn handler register? ";
        this.jfxdndHandler = jfxdndHandler;
    }*/
}
