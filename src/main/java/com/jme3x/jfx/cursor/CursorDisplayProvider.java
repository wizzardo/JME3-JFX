package com.jme3x.jfx.cursor;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;

/**
 * The interface for implementing the provider of cursors.
 */
public interface CursorDisplayProvider {

    /**
     * Setups the type of cursor.
     */
    void setupCursor(CursorType normal);

    /**
     * Shows ths cursor.
     */
    void showCursor(CursorFrame cursorFrame);
}
