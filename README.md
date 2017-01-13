JME3-JFX
========

JFX Gui bridge for JME with usefull utilities for common usecases

License is the New BSD License (same as JME3) 
http://opensource.org/licenses/BSD-3-Clause

How to use:
* 1. You need to create FXContainer inside the method simpleInitApp() of SimpleApplication.

````
final ProtonCursorProvider cursorProvider = new ProtonCursorProvider(this, assetManager, inputManager);

for (final CursorType type : CursorType.values()) {
    cursorProvider.setup(type);
}

fxContainer = JmeFxContainer.install(this, guiNode, cursorProvider);
````

* 2. Then you can get javaFX scene from the fx container and use it to build your UI:
````
container.getScene()
````
* 3. Also you need to add calling a draw method in the main loop(the method update() of your application)

````
if (fxContainer.isNeedWriteToJME()) fxContainer.writeToJME();
````