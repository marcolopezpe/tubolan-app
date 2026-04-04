package pe.marcolopez.apps.tubolan;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApp extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Italic-VariableFont_opsz,wght.ttf"), 14);
    Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-VariableFont_opsz,wght.ttf"), 14);
  }
}
