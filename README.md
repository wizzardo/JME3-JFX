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

if you want to use simple default arrow cursor you should use this:
````
fxContainer = JmeFxContainer.install(this, guiNode, null);
````

* 2. Then you must create javaFX scene and set it on the fx container to build your UI.
You can use FXML if you prefer it like this:
````
FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/hud.fxml"));
BorderPane root = fxmlLoader.load();
int width = app.getContext().getSettings().getWidth();
int height = app.getContext().getSettings().getHeight();
root.setPrefSize(width, height);
Hud hud = fxmlLoader.getController();
Group group = new Group(root);
Scene scene = new Scene(group, Color.TRANSPARENT);
fxContainer.setScene(scene, group);
````

where Hud is controller class for example

````
public class Hud implements Initializable {
    @FXML
    public BorderPane root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
````
Ofc hud.fxml should be accessible in resources, valid and points to Hud class.
If you do not want to use FXML your code can be even simpler:

````
BorderPane root = new BorderPane();
int width = app.getContext().getSettings().getWidth();
int height = app.getContext().getSettings().getHeight();
root.setPrefSize(width, height);
Group group = new Group(root);
Scene scene = new Scene(group, Color.TRANSPARENT);
fxContainer.setScene(scene, group);
````

* 3. From now you are able to get transparent JavaFX scene and use it as in classic JavaFX application.
````
fxContainer.getScene();
````

* 3. Also you need to add calling a draw method in the main loop(the method update() of your application)

````
if (fxContainer.isNeedWriteToJME()) fxContainer.writeToJME();
````