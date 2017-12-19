package com.jme3x.jfx.injme.cursor;

import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;

public interface CursorDisplayProvider {

    /**
     * called by the GuiManager during startup, should be used to create the necessary cursors
     */
    void setup(CursorType normal);

    void showCursor(CursorFrame cursorFrame);
}
