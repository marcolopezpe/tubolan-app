package pe.marcolopez.apps.tubolan.views;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.Dependent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryBroadcaster;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryListener;
import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;

import java.util.function.Consumer;

@FxView
@Dependent
public class HomeController {

  @FXML
  Parent root;

  @FXML
  Label lblDeviceName;

  @FXML
  Label lblDeviceStatus;

  @FXML
  VBox vboxConnectedDevices;

  @FXML
  public void initialize() {
    String deviceName = DeviceInfoUtil.getDeviceName();
    String deviceIp = DeviceInfoUtil.getRealLocalIp();

    lblDeviceName.setText(deviceName);
    lblDeviceStatus.setText("Online (" + deviceIp + ")");

    LanDiscoveryListener.start(this);
    LanDiscoveryBroadcaster.start(deviceName, deviceIp);

    Stage stage = new Stage();
    stage.setResizable(false);
    stage.setOnCloseRequest(_ -> {
      Platform.exit();
      System.exit(0);
    });

    Scene scene = new Scene(this.root);
    stage.setScene(scene);
    stage.show();
  }

  public void addConnectedDevice(String deviceName, String deviceIp, Consumer<HBox> callback) {
    Platform.runLater(() -> {
      HBox deviceBox = new HBox(10);
      deviceBox.getStyleClass().add("device-item");
      deviceBox.setAlignment(Pos.CENTER_LEFT);

      StackPane statusDot = new StackPane();
      statusDot.getStyleClass().addAll("status-dot-device", "status-online");

      Label lblName = new Label(deviceName);
      lblName.getStyleClass().add("device-name");

      deviceBox.getChildren().addAll(statusDot, lblName);
      vboxConnectedDevices.getChildren().add(deviceBox);

      if (callback != null) {
        callback.accept(deviceBox);
      }
    });
  }

  public void removeConnectedDevice(HBox deviceBox) {
    vboxConnectedDevices.getChildren().remove(deviceBox);
  }
}
