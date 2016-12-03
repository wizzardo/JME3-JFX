package com.jme3x.jfx.injfx.input;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3x.jfx.injfx.JmeOffscreenSurfaceContext;

import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import rlib.util.linkedlist.LinkedList;

import static rlib.util.linkedlist.LinkedListFactory.newLinkedList;

/**
 * The implementation of the {@link MouseInput} for using in the ImageView.
 *
 * @author JavaSaBr.
 */
public class JFXMouseInput extends JFXInput implements MouseInput {

    private static final int WHEEL_SCALE = 10;

    private final EventHandler<MouseEvent> processMotion = this::processMotion;
    private final EventHandler<MouseEvent> processPressed = this::processPressed;
    private final EventHandler<MouseEvent> processReleased = this::processReleased;
    private final EventHandler<ScrollEvent> processScroll = this::processScroll;

    private final LinkedList<MouseMotionEvent> mouseMotionEvents;
    private final LinkedList<MouseButtonEvent> mouseButtonEvents;

    private int mouseX;
    private int mouseY;
    private int mouseWheel;

    public JFXMouseInput(final JmeOffscreenSurfaceContext context) {
        super(context);
        mouseMotionEvents = newLinkedList(MouseMotionEvent.class);
        mouseButtonEvents = newLinkedList(MouseButtonEvent.class);
    }

    @Override
    public void bind(final ImageView imageView) {
        super.bind(imageView);
        imageView.setOnMouseMoved(processMotion);
        imageView.setOnMousePressed(processPressed);
        imageView.setOnMouseReleased(processReleased);
        imageView.setOnScroll(processScroll);
    }

    @Override
    public void unbind() {
        super.unbind();
        imageView.setOnMouseMoved(null);
        imageView.setOnMousePressed(null);
        imageView.setOnMouseReleased(null);
        imageView.setOnScroll(null);
    }

    @Override
    protected void updateImpl() {
        while (!mouseMotionEvents.isEmpty()) {
            listener.onMouseMotionEvent(mouseMotionEvents.poll());
        }
        while (!mouseButtonEvents.isEmpty()) {
            listener.onMouseButtonEvent(mouseButtonEvents.poll());
        }
    }

    private void processScroll(final ScrollEvent mouseEvent) {
        onWheelScroll(mouseEvent.getDeltaX() * WHEEL_SCALE, mouseEvent.getDeltaY() * WHEEL_SCALE);
    }

    private void processReleased(final MouseEvent mouseEvent) {
        onMouseButton(mouseEvent.getButton(), false);
    }

    private void processPressed(final MouseEvent mouseEvent) {
        onMouseButton(mouseEvent.getButton(), true);
    }

    private void processMotion(final MouseEvent mouseEvent) {
        onCursorPos(mouseEvent.getSceneX(), mouseEvent.getSceneY());
    }

    private void onWheelScroll(final double xOffset, final double yOffset) {

        mouseWheel += yOffset;

        final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(mouseX, mouseY, 0, 0, mouseWheel, (int) Math.round(yOffset));
        mouseMotionEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(() -> mouseMotionEvents.add(mouseMotionEvent));
    }

    private void onCursorPos(final double xpos, final double ypos) {

        int xDelta;
        int yDelta;
        int x = (int) Math.round(xpos);
        int y = context.getHeight() - (int) Math.round(ypos);

        if (mouseX == 0) mouseX = x;
        if (mouseY == 0) mouseY = y;

        xDelta = x - mouseX;
        yDelta = y - mouseY;

        mouseX = x;
        mouseY = y;

        if (xDelta == 0 && yDelta == 0) return;

        final MouseMotionEvent mouseMotionEvent = new MouseMotionEvent(x, y, xDelta, yDelta, mouseWheel, 0);
        mouseMotionEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(() -> mouseMotionEvents.add(mouseMotionEvent));
    }

    private void onMouseButton(final MouseButton button, final boolean pressed) {

        final MouseButtonEvent mouseButtonEvent = new MouseButtonEvent(convertButton(button), pressed, mouseX, mouseY);
        mouseButtonEvent.setTime(getInputTimeNanos());

        EXECUTOR.addToExecute(() -> mouseButtonEvents.add(mouseButtonEvent));
    }

    private int convertButton(final MouseButton button) {
        switch (button) {
            case PRIMARY: {
                return MouseInput.BUTTON_LEFT;
            }
            case MIDDLE: {
                return MouseInput.BUTTON_MIDDLE;
            }
            case SECONDARY: {
                return MouseInput.BUTTON_RIGHT;
            }
            default: {
                return 0;
            }
        }
    }

    @Override
    public void setCursorVisible(final boolean visible) {
    }

    @Override
    public int getButtonCount() {
        return 3;
    }

    @Override
    public void setNativeCursor(final JmeCursor cursor) {
    }
}
