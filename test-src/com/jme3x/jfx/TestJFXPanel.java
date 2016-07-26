package com.jme3x.jfx;

import com.jme3.app.SimpleApplication;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;

/**
 * Created by ronn on 22.07.16.
 */
public class TestJFXPanel extends SimpleApplication {

    private JmeFxContainer container;

    public static void main(String[] args) {

        // фикс рендера шрифтов в FX
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");

        // настройки для JavaFX
        System.setProperty("prism.vsync", "true");
        System.setProperty("javafx.animation.fullspeed", "false");
        System.setProperty("prism.cacheshapes", "true");

        final AppSettings settings = new AppSettings(true);
        settings.setRenderer("CUSTOM" + TestContext.class.getName());
        settings.setResizable(true);
        settings.setFrameRate(90);

        TestJFXPanel test = new TestJFXPanel();
        test.setSettings(settings);
        test.setShowSettings(false);
        test.start();
    }

    @Override
    public void simpleInitApp() {

        final Node guiNode = getGuiNode();
        guiNode.detachAllChildren();

        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);

        container = JmeFxContainer.install(this, guiNode, null);

        final Button button = new Button("WEFWEWEFWEFWE");
        final TextField textField = new TextField("TextField");
        final HTMLEditor htmlEditor = new HTMLEditor();

        final Group group = new Group(new VBox(button, textField, htmlEditor));
        final Scene scene = new Scene(group);

        container.setScene(scene);
    }

    @Override
    public void update() {
        if(container.isNeedWriteToJME()) container.writeToJME();
        super.update();
    }
}
