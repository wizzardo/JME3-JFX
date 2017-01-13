package com.jme3x.jfx;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3x.jfx.cursor.proton.ProtonCursorProvider;

import rlib.logging.LoggerManager;

public class TestFX extends SimpleApplication {

    public static void main(final String[] args) {

        LoggerManager.getDefaultLogger();

        final AppSettings settings = new AppSettings(true);

        final TestFX testFX = new TestFX();
        testFX.setSettings(settings);
        testFX.setShowSettings(false);
        testFX.start();
    }

    @Override
    public void simpleInitApp() {

        ProtonCursorProvider protonCursorProvider = new ProtonCursorProvider(this, assetManager, inputManager);

        JmeFxContainer.install(this, guiNode, protonCursorProvider);
    }
}
