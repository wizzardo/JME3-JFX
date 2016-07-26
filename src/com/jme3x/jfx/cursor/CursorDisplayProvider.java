package com.jme3x.jfx.cursor;


import java.awt.*;

/**
 * The interface for implementing the provider of cursors.
 */
public interface CursorDisplayProvider {

    /**
     * Setups the type of cursor.
     */
    void setupCursor(Cursor normal);

    /**
     * Shows ths cursor.
     */
    void showCursor(Cursor cursorFrame);
}
