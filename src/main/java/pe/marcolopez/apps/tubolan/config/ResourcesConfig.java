package pe.marcolopez.apps.tubolan.config;

import io.quarkiverse.fx.FxApplication;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ResourcesConfig extends FxApplication {

  @Override
  public void start(Stage primaryStage) {
    Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-Italic-VariableFont_opsz,wght.ttf"), 14);
    Font.loadFont(getClass().getResourceAsStream("/fonts/Inter-VariableFont_opsz,wght.ttf"), 14);
  }
}
