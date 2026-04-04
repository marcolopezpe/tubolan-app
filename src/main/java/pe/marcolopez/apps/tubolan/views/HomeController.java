package pe.marcolopez.apps.tubolan.views;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.Dependent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pe.marcolopez.apps.tubolan.models.Device;
import pe.marcolopez.apps.tubolan.networks.FileSender;
import pe.marcolopez.apps.tubolan.runneables.FileReceiverServer;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryBroadcaster;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryListener;
import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;

import java.io.File;
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
  Label lblCurrentTarget;

  @FXML
  StackPane stackDropZone;

  HBox selectedDeviceBox;

  @FXML
  public void initialize() {
    String deviceName = DeviceInfoUtil.getDeviceName();
    String deviceIp = DeviceInfoUtil.getRealLocalIp();

    lblDeviceName.setText(deviceName);
    lblDeviceStatus.setText("Online (" + deviceIp + ")");

    LanDiscoveryListener.start(this);
    LanDiscoveryBroadcaster.start(deviceName, deviceIp);
    FileReceiverServer.startServer();

    Stage stage = new Stage();
    stage.setResizable(false);
    stage.setOnCloseRequest(_ -> {
      Platform.exit();
      System.exit(0);
    });

    Scene scene = new Scene(this.root);
    stage.setScene(scene);
    stage.show();

    setupFileDropZone();
  }

  public void addConnectedDevice(Device device, Consumer<HBox> callback) {
    Platform.runLater(() -> {
      HBox deviceBox = new HBox(10);
      deviceBox.getStyleClass().add("device-item");
      deviceBox.setAlignment(Pos.CENTER_LEFT);
      deviceBox.getProperties().put("device", device);

      StackPane statusDot = new StackPane();
      statusDot.getStyleClass().addAll("status-dot-device", "status-online");

      Label lblName = new Label(device.getName());
      lblName.getStyleClass().add("device-name");

      deviceBox.getChildren().addAll(statusDot, lblName);
      vboxConnectedDevices.getChildren().add(deviceBox);
      deviceBox.setOnMouseClicked(e -> selectDevice(deviceBox));

      if (callback != null) {
        callback.accept(deviceBox);
      }
    });
  }

  private void selectDevice(HBox deviceBox) {
    if (selectedDeviceBox != null) {
      selectedDeviceBox.getStyleClass().remove("device-item-selected");
    }
    selectedDeviceBox = deviceBox;
    selectedDeviceBox.getStyleClass().add("device-item-selected");

    Device device = (Device) deviceBox.getProperties().get("device");
    if (device != null) {
      lblCurrentTarget.setText(device.toDiplayFull());
    }
  }

  public void removeConnectedDevice(HBox deviceBox) {
    vboxConnectedDevices.getChildren().remove(deviceBox);
  }

  private void setupFileDropZone() {
    stackDropZone.setOnDragOver(event -> {
      if (event.getDragboard().hasFiles()) {
        event.acceptTransferModes(TransferMode.COPY);
      }
      event.consume();
    });

    stackDropZone.setOnDragDropped(event -> {
      var dragboard = event.getDragboard();
      if (dragboard.hasFiles()) {
        dragboard.getFiles().forEach(file -> {
          IO.println("Archivo arrastrado: " + file.getAbsolutePath());
          sendFileToSelectedDevice(file);
        });
      }
      event.setDropCompleted(true);
      event.consume();
    });

    stackDropZone.setOnMouseClicked(event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Selecciona un archivo");
      File file = fileChooser.showOpenDialog(stackDropZone.getScene().getWindow());
      if (file != null) {
        IO.println("Archivo seleccionado: " + file.getAbsolutePath());
        sendFileToSelectedDevice(file);
      }
    });
  }

  private void sendFileToSelectedDevice(File file) {
    Device device = (Device) selectedDeviceBox.getProperties().get("device");
    if (device != null) {
      FileSender.sendFile(device.getIp(), file);
    }
  }
}
