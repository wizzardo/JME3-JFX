/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3x.jfx.injme.input;

import static java.util.Objects.requireNonNull;
import com.jme3.app.Application;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.event.*;
import com.jme3x.jfx.injme.JmeFxContainer;
import com.jme3x.jfx.injme.JmeFxDNDHandler;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.util.BitSet;

/**
 * Converts JMEEvents to JFXEvents
 *
 * @author Heist, JavaSaBr
 */
public class JmeFXInputListener implements RawInputListener {

    /**
     * The javaFX container.
     */
    @NotNull
    private final JmeFxContainer jmeFxContainer;

    /**
     * The key state set.
     */
    @NotNull
    private final BitSet keyStateSet;

    /**
     * The key char array.
     */
    @NotNull
    private final char[][] keyCharArray;

    /**
     * The key char set.
     */
    @NotNull
    private final char[] keyCharSet;

    /**
     * The mouse button states.
     */
    @NotNull
    private final boolean[] mouseButtonState;

    /**
     * The raw input listener.
     */
    @Nullable
    private volatile RawInputListener rawInputListener;

    /**
     * The D&D handler.
     */
    @Nullable
    private volatile JmeFxDNDHandler jfxdndHandler;

    public JmeFXInputListener(@NotNull final JmeFxContainer listensOnContainer) {
        this.jmeFxContainer = listensOnContainer;
        this.keyStateSet = new BitSet(0xFF);
        this.keyCharSet = new char[Character.MAX_CODE_POINT];
        this.mouseButtonState = new boolean[3];
        this.keyCharArray = new char[Character.MAX_CODE_POINT][];

        for (int i = 0, length = keyCharArray.length; i < length; i++) {
            keyCharArray[i] = new char[]{(char) i};
        }
    }

    /**
     * @return The D&D handler.
     */
    @Nullable
    private JmeFxDNDHandler getJmeFxDNDHandler() {
        return jfxdndHandler;
    }

    @Override
    public void beginInput() {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.beginInput();
    }

    @Override
    public void endInput() {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.endInput();
    }

    /**
     * @return the raw input listener.
     */
    @Nullable
    private RawInputListener getRawInputListener() {
        return rawInputListener;
    }

    /**
     * @return The javaFX container.
     */
    @NotNull
    private JmeFxContainer getJmeFxContainer() {
        return jmeFxContainer;
    }

    /**
     * @return the key char array.
     */
    @NotNull
    private char[][] getKeyCharArray() {
        return keyCharArray;
    }

    /**
     * @return the key char set.
     */
    @NotNull
    private char[] getKeyCharSet() {
        return keyCharSet;
    }

    /**
     * @return the key state set.
     */
    @NotNull
    private BitSet getKeyStateSet() {
        return keyStateSet;
    }

    /**
     * @return the mouse button states.
     */
    @NotNull
    private boolean[] getMouseButtonState() {
        return mouseButtonState;
    }

    @Override
    public void onJoyAxisEvent(@NotNull final JoyAxisEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onJoyAxisEvent(event);
    }

    @Override
    public void onJoyButtonEvent(@NotNull final JoyButtonEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onJoyButtonEvent(event);
    }

    @Override
    public void onKeyEvent(@NotNull final KeyInputEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onKeyEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final EmbeddedSceneInterface scenePeer = jmeFxContainer.getScenePeer();
        if (scenePeer == null) return;

        final BitSet keyStateSet = getKeyStateSet();

        final char[][] keyCharArray = getKeyCharArray();
        final char[] keyCharSet = getKeyCharSet();
        final char keyChar = event.getKeyChar();

        final int keyCode = event.getKeyCode();

        int fxKeyCode = keyCode == KeyInput.KEY_UNKNOWN ? KeyEvent.VK_UNDEFINED : AwtKeyInput.convertJmeCode(keyCode);

        final int keyState = retrieveKeyState();

        if (fxKeyCode > keyCharSet.length) {
            switch (keyChar) {
                case '\\': {
                    fxKeyCode = KeyEvent.VK_BACK_SLASH;
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
    public void onMouseButtonEvent(@NotNull final MouseButtonEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onMouseButtonEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final Application application = requireNonNull(jmeFxContainer.getApplication());
        final InputManager inputManager = application.getInputManager();

        if (jmeFxContainer.getScenePeer() == null) {
            return;
        }

        final Scene scene = requireNonNull(jmeFxContainer.getScene());

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

        final boolean covered = jmeFxContainer.isCovered(x, y);

        if (!covered) {
            jmeFxContainer.loseFocus();
        } else if (inputManager.isCursorVisible()) {
            event.setConsumed();
            jmeFxContainer.grabFocus();
        }

        int type;

        if (event.isPressed()) {
            type = AbstractEvents.MOUSEEVENT_PRESSED;
        } else if (event.isReleased()) {
            type = AbstractEvents.MOUSEEVENT_RELEASED;
        } else {
            return;
        }

        if (inputManager.isCursorVisible() || event.isReleased()) {
            Platform.runLater(() -> onMouseButtonEventImpl(x, y, button, type));
        }
    }

    private void onMouseButtonEventImpl(int x, int y, int button, int type) {

        final boolean[] mouseButtonState = getMouseButtonState();
        final JmeFxDNDHandler jfxdndHandler = getJmeFxDNDHandler();

        final boolean primaryBtnDown = mouseButtonState[0];
        final boolean middleBtnDown = mouseButtonState[1];
        final boolean secondaryBtnDown = mouseButtonState[2];

        if (jfxdndHandler != null) {
            jfxdndHandler.mouseUpdate(x, y, primaryBtnDown);
        }

        final JmeFxContainer fxContainer = getJmeFxContainer();
        final EmbeddedSceneInterface scenePeer = requireNonNull(fxContainer.getScenePeer());

        final int screenX = fxContainer.getOldX() + x;
        final int screenY = fxContainer.getOldY() + y;

        final BitSet keyStateSet = getKeyStateSet();

        final boolean shift = keyStateSet.get(KeyEvent.VK_SHIFT);
        final boolean ctrl = keyStateSet.get(KeyEvent.VK_CONTROL);
        final boolean alt = keyStateSet.get(KeyEvent.VK_ALT);
        final boolean meta = keyStateSet.get(KeyEvent.VK_META);
        final boolean popupTrigger = button == AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;

        scenePeer.mouseEvent(type, button, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y, screenX, screenY,
                shift, ctrl, alt, meta, 0, popupTrigger);
    }

    @Override
    public void onMouseMotionEvent(final MouseMotionEvent event) {

        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onMouseMotionEvent(event);

        final JmeFxContainer jmeFxContainer = getJmeFxContainer();
        final Application application = requireNonNull(jmeFxContainer.getApplication());
        final InputManager inputManager = requireNonNull(application.getInputManager());

        if (jmeFxContainer.getScenePeer() == null) {
            return;
        }

        final Scene scene = requireNonNull(jmeFxContainer.getScene());

        final int x = event.getX();
        final int y = (int) Math.round(scene.getHeight()) - event.getY();

        final boolean covered = jmeFxContainer.isCovered(x, y);

        if (covered) {
            event.setConsumed();
        }

        final boolean[] mouseButtonState = getMouseButtonState();
        // not sure if should be grabbing focus on mouse motion event
        // grabFocus();

        int type = AbstractEvents.MOUSEEVENT_MOVED;
        int button = AbstractEvents.MOUSEEVENT_NONE_BUTTON;

        final int wheelRotation = (int) Math.round(event.getDeltaWheel() / -120.0);

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

        if (inputManager.isCursorVisible()) {
            Platform.runLater(() -> onMouseMotionEventImpl(x, y, wheelRotation, ftype, fbutton));
        }
    }

    private void onMouseMotionEventImpl(int x, int y, int wheelRotation, int ftype, int fbutton) {

        final JmeFxContainer fxContainer = getJmeFxContainer();
        final Application application = requireNonNull(fxContainer.getApplication());
        final InputManager inputManager = application.getInputManager();

        if (!inputManager.isCursorVisible()) {
            return;
        }

        final JmeFxDNDHandler dndHandler = getJmeFxDNDHandler();
        final boolean[] mouseButtonState = getMouseButtonState();

        final boolean primaryBtnDown = mouseButtonState[0];
        final boolean middleBtnDown = mouseButtonState[1];
        final boolean secondaryBtnDown = mouseButtonState[2];

        if (dndHandler != null) {
            dndHandler.mouseUpdate(x, y, primaryBtnDown);
        }

        final EmbeddedSceneInterface scenePeer = requireNonNull(fxContainer.getScenePeer());

        final int screenX = fxContainer.getOldX() + x;
        final int screenY = fxContainer.getOldY() + y;

        final BitSet keyStateSet = getKeyStateSet();

        final boolean shift = keyStateSet.get(KeyEvent.VK_SHIFT);
        final boolean ctrl = keyStateSet.get(KeyEvent.VK_CONTROL);
        final boolean alt = keyStateSet.get(KeyEvent.VK_ALT);
        final boolean meta = keyStateSet.get(KeyEvent.VK_META);

        scenePeer.mouseEvent(ftype, fbutton, primaryBtnDown, middleBtnDown, secondaryBtnDown, x, y, screenX, screenY,
                shift, ctrl, alt, meta, wheelRotation, false);
    }

    @Override
    public void onTouchEvent(@NotNull final TouchEvent event) {
        final RawInputListener adapter = getRawInputListener();
        if (adapter != null) adapter.onTouchEvent(event);
    }

    private int retrieveKeyState() {

        int embedModifiers = 0;

        final BitSet keyStateSet = getKeyStateSet();

        if (keyStateSet.get(KeyEvent.VK_SHIFT)) {
            embedModifiers |= AbstractEvents.MODIFIER_SHIFT;
        }

        if (keyStateSet.get(KeyEvent.VK_CONTROL)) {
            embedModifiers |= AbstractEvents.MODIFIER_CONTROL;
        }

        if (keyStateSet.get(KeyEvent.VK_ALT)) {
            embedModifiers |= AbstractEvents.MODIFIER_ALT;
        }

        if (keyStateSet.get(KeyEvent.VK_META)) {
            embedModifiers |= AbstractEvents.MODIFIER_META;
        }

        return embedModifiers;
    }

    public void setEverListeningRawInputListener(@NotNull final RawInputListener rawInputListenerAdapter) {
        this.rawInputListener = rawInputListenerAdapter;
    }

    /**
     * set on drag start /nulled on end<br> necessary so that the drag events can be generated
     * appropiatly
     */
    public void setMouseDNDListener(@Nullable final JmeFxDNDHandler jfxdndHandler) {
        assert this.jfxdndHandler == null || jfxdndHandler == null : "duplicate jfxdndn handler register? ";
        this.jfxdndHandler = jfxdndHandler;
    }
}
