JME3-JFX
========

JFX Gui bridge for JME with usefull utilities for common usecases

License is the New BSD License (same as JME3) 
http://opensource.org/licenses/BSD-3-Clause

## How to add the library to your project

#### Gradle

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    compile 'com.github.JavaSaBr:JME3-JFX:1.7.0'
}
```

    
#### Maven

```xml
<repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.JavaSaBr</groupId>
        <artifactId>JME3-JFX</artifactId>
        <version>1.7.0</version>
    </dependency>
```

#### How to integrate jME application to JavaFX ImageView:

```java

    final ImageView imageView = new ImageView();
        
    final AppSettings settings = JmeToJFXIntegrator.prepareSettings(new AppSettings(true), 60);
    final JmeToJFXApplication application = new MySomeApplication();
    
    JmeToJFXIntegrator.startAndBindMainViewPort(application, imageView, Thread::new);
```

#### How to integrate javaFX UI to jME application:

```java

    public class MyApplication extends SimpleApplication {
    
        private JmeFxContainer container;
        
        @Override
        public void simpleInitApp() {
            container = JmeFxContainer.install(this, getGuiNode());
    
            final Button button = new Button("BUTTON");
            final Group rootNode = new Group(button);
            final Scene scene = new Scene(rootNode, 600, 600);
            scene.setFill(Color.TRANSPARENT);

            container.setScene(scene, rootNode);
            
            getInputManager().setCursorVisible(true);
        }
    
        @Override
        public void simpleUpdate(final float tpf) {
            super.simpleUpdate(tpf);
            // we decide here that we need to do transferring the last frame from javaFX to jME
            if (container.isNeedWriteToJme()) {
                container.writeToJme();
            }
        }
    }
```

Also, you can look at some examples in the tests package:

* [jME Application is inside jFX Canvas](https://github.com/JavaSaBr/JME3-JFX/blob/master/src/test/java/com/jme3x/jfx/TestJmeToJFXCanvas.java)
* [jME Application is inside jFX ImageView](https://github.com/JavaSaBr/JME3-JFX/blob/master/src/test/java/com/jme3x/jfx/TestJmeToJFXImageView.java)
* [JavaFX Scene is inside jME Application](https://github.com/JavaSaBr/JME3-JFX/blob/master/src/test/java/com/jme3x/jfx/TestJavaFxInJme.java)
