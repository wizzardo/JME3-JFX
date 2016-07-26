package com.jme3x.jfx.cursor.proton;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.jme3x.jfx.cursor.CursorDisplayProvider;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http://www.rw-designer.com/cursor-set/proton by juanello <br> A cursorProvider that simulates the
 * native JFX one and tries to behave similar,<br> using native cursors and 2D surface logic.
 *
 * @author empire
 */
public class ProtonCursorProvider implements CursorDisplayProvider {

    private ConcurrentHashMap<Cursor, JmeCursor> cache = new ConcurrentHashMap<>();

    private AssetManager assetManager;
    private InputManager inputManager;
    private Application app;

    public ProtonCursorProvider(final Application app, final AssetManager assetManager, final InputManager inputManager) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.app = app;
        assetManager.registerLocator("", ClasspathLocator.class);
    }

    @Override
    public synchronized void showCursor(final Cursor cursor) {

        if (cache.get(cursor) == null) {
            setupCursor(cursor);
        }

        final JmeCursor toDisplay = cache.get(cursor);
        if (toDisplay == null) return;

        app.enqueue(() -> {
            inputManager.setMouseCursor(toDisplay);
            return null;
        });
    }

    @Override
    public void setupCursor(final Cursor cursor) {

        JmeCursor loaded = null;

        switch (cursor.getType()) {
            case Cursor.CROSSHAIR_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_cross.cur");
                break;
            case Cursor.DEFAULT_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_arrow.cur");
                break;
            case Cursor.MOVE_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_move.cur");
                break;
            case Cursor.HAND_CURSOR:
                break;
            case Cursor.TEXT_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_text.cur");
                break;
            case Cursor.WAIT_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_busy.ani");
                break;
            case Cursor.W_RESIZE_CURSOR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ew.cur");
                break;
            default: {
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_arrow.cur");
            }
        }

        if (loaded != null) cache.put(cursor, loaded);
    }
}
