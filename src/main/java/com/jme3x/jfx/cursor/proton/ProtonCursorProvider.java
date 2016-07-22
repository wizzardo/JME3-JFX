package com.jme3x.jfx.cursor.proton;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.InputManager;
import com.jme3x.jfx.cursor.CursorDisplayProvider;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.cursor.CursorType;

import java.util.concurrent.ConcurrentHashMap;

import rlib.logging.Logger;
import rlib.logging.LoggerManager;

/**
 * http://www.rw-designer.com/cursor-set/proton by juanello <br> A cursorProvider that simulates the
 * native JFX one and tries to behave similar,<br> using native cursors and 2D surface logic.
 *
 * @author empire
 */
public class ProtonCursorProvider implements CursorDisplayProvider {

    private static final Logger LOGGER = LoggerManager.getLogger(ProtonCursorProvider.class);

    private ConcurrentHashMap<CursorType, JmeCursor> cache = new ConcurrentHashMap<>();

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
    public void setupCursor(final CursorType cursorType) {

        JmeCursor loaded = null;

        switch (cursorType) {
            case CLOSED_HAND:
                break;
            case CROSSHAIR:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_cross.cur");
                break;
            case DEFAULT:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_arrow.cur");
                break;
            case DISAPPEAR:
                break;
            case E_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ew.cur");
                break;
            case HAND:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_link.cur");
                break;
            case H_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ew.cur");
                break;
            case IMAGE:
                break;
            case MOVE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_move.cur");
                break;
            case NE_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_nesw.cur");
                break;
            case NONE:
                break;
            case NW_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_nwse.cur");
                break;
            case N_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ns.cur");
                break;
            case OPEN_HAND:
                break;
            case SE_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_nwse.cur");
                break;
            case SW_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_nesw.cur");
                break;
            case S_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ns.cur");
                break;
            case TEXT:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_text.cur");
                break;
            case V_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ns.cur");
                break;
            case WAIT:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_busy.ani");
                break;
            case W_RESIZE:
                loaded = (JmeCursor) assetManager.loadAsset("com/jme3x/jfx/cursor/proton/aero_ew.cur");
                break;
        }

        if (loaded != null) cache.put(cursorType, loaded);
    }

    @Override
    public void showCursor(final CursorFrame cursorFrame) {

        CursorType cursorType = cursorFrame.getCursorType();

        if (cache.get(cursorType) == null) {
            LOGGER.debug("Unkown Cursor! " + cursorType);
            cursorType = CursorType.DEFAULT;
        }

        final JmeCursor toDisplay = cache.get(cursorType);
        if (toDisplay == null) return;

        app.enqueue(() -> {
            inputManager.setMouseCursor(toDisplay);
            return null;
        });
    }
}
