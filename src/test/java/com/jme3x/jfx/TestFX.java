package com.jme3x.jfx;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3x.jfx.cursor.proton.ProtonCursorProvider;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import rlib.logging.LoggerLevel;
import rlib.logging.LoggerManager;

public class TestFX extends SimpleApplication {

    private JmeFxContainer container;

    public static void main(final String[] args) {

        LoggerManager.getDefaultLogger();

        LoggerLevel.WARNING.setEnabled(true);

        final AppSettings settings = new AppSettings(true);

        final TestFX testFX = new TestFX();
        testFX.setSettings(settings);
        testFX.setShowSettings(false);
        testFX.start();
    }

    @Override
    public void simpleInitApp() {

        final ProtonCursorProvider protonCursorProvider = new ProtonCursorProvider(this, assetManager, inputManager);
        container = JmeFxContainer.install(this, guiNode, protonCursorProvider);

        flyCam.setEnabled(true);
        flyCam.setDragToRotate(true);

        final Group root = new Group(new VBox(new Button("wefwefW"), new Button("wefwefW")));
        final Scene newScene = new Scene(root, Color.ALICEBLUE);

        container.setScene(newScene, root);
    }

    @Override
    public void update() {
        super.update();
        if(container.isNeedWriteToJME()) {
            container.writeToJME();
        }
    }
}
