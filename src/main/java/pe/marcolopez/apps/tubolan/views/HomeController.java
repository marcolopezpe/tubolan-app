package pe.marcolopez.apps.tubolan.views;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import pe.marcolopez.apps.tubolan.models.Device;
import pe.marcolopez.apps.tubolan.models.FileTransfer;
import pe.marcolopez.apps.tubolan.networks.DeviceNetwork;
import pe.marcolopez.apps.tubolan.networks.FileSender;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryListener;
import pe.marcolopez.apps.tubolan.config.DeviceSession;
import java.io.File;
import java.util.Objects;

import static javafx.scene.control.Alert.AlertType.ERROR;
import static pe.marcolopez.apps.tubolan.utils.ControlsUtil.*;

@Slf4j
@FxView
@Dependent
public class HomeController {

  @Inject
  DeviceSession sessionData;

  @Inject
  LanDiscoveryListener lanDiscoveryListener;

  @Inject
  FileSender fileSender;

  @Inject
  DeviceNetwork deviceNetwork;

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

  @FXML
  VBox vboxFileTransfers;

  HBox deviceBoxSelected;

  @FXML
  Button btnRefresh;

  @FXML
  StackPane refreshIconContainer;

  @FXML
  public void initialize() {
    lblDeviceName.setText(sessionData.getDevice().getName());
    lblDeviceStatus.setText("Online (" + sessionData.getDevice().getIp() + ")");

    this.refreshConnectedDevices();

    var stage = new Stage();
    stage.setResizable(false);
    stage.setOnCloseRequest(_ -> {
      Platform.exit();
      System.exit(0);
    });

    var scene = new Scene(this.root);
    stage.setScene(scene);
    stage.show();

    this.configureFileDropZone();
  }

  private void refreshConnectedDevices() {
    lanDiscoveryListener.getConnectedDevices()
        .addListener((MapChangeListener<String, Device>) change ->
            Platform.runLater(() -> {
              if (change.wasAdded()) {
                var newDevice = change.getValueAdded();
                var newDeviceBox = createDeviceBox(newDevice, this::selectDevice);
                vboxConnectedDevices.getChildren().add(newDeviceBox);
              }

              if (change.wasRemoved()) {
                var removedDevice = change.getValueRemoved();
                vboxConnectedDevices.getChildren().removeIf(node -> {
                  var deviceBox = (HBox) node;
                  var device = (Device) deviceBox.getProperties().get("device");
                  return device != null && device.getIp().equals(removedDevice.getIp());
                });
              }
            }));
  }

  private void selectDevice(HBox deviceBox) {
    if (deviceBoxSelected != null) {
      deviceBoxSelected.getStyleClass().remove("device-item-selected");
    }
    deviceBoxSelected = deviceBox;
    deviceBoxSelected.getStyleClass().add("device-item-selected");

    var deviceSelected = (Device) deviceBox.getProperties().get("device");
    if (deviceSelected != null) {
      lblCurrentTarget.setText(deviceSelected.toDisplayFull());
    }
  }

  private void configureFileDropZone() {
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
          log.info("### File dropped: {}", file.getAbsolutePath());
          this.sendFileToSelectedDevice(file);
        });
      }
      event.setDropCompleted(true);
      event.consume();
    });

    stackDropZone.setOnMouseClicked(_ -> {
      var fileChooser = new FileChooser();
      fileChooser.setTitle("Seleccionar archivo");
      var file = fileChooser.showOpenDialog(stackDropZone.getScene().getWindow());
      if (file != null) {
        log.info("### File selected: {}", file.getAbsolutePath());
        this.sendFileToSelectedDevice(file);
      }
    });
  }

  private void sendFileToSelectedDevice(File file) {
    if (deviceBoxSelected == null) {
      log.error("### No hay dispositivo seleccionado.");
      showMessageDialog("Error", "No hay dispositivo seleccionado", ERROR);
      return;
    }

    var device = (Device) deviceBoxSelected.getProperties().get("device");
    if (device == null) {
      log.error("### No hay dispositivo seleccionado.");
      showMessageDialog("Error", "No hay dispositivo seleccionado", ERROR);
      return;
    }

    var fileTransfer = new FileTransfer(file);
    fileTransfer.setTargetDevice(device);

    var fileCard = createFileCard(fileTransfer);

    Platform.runLater(() -> vboxFileTransfers.getChildren().addFirst(fileCard));

    log.info("### Trying to send file to {} ({})...", device.getName(), device.getIp());
    log.info("### Checking reachability of {}...", device.getIp());

    if (!this.isDeviceStillOnline(device)) {
      fileTransfer.setStatus(FileTransfer.Status.FAILED);
      fileTransfer.setErrorMessage("El dispositivo se ha desconectado");
      fileTransfer.setErrorDetails("No se pudo establecer conexion con el dispositivo");

      updateFileCard(
          fileCard,
          1.0,
          0,
          "00:00s",
          FileTransfer.Status.FAILED,
          () -> {
            log.info("### View details");
          }
      );

      log.error("### Device {} is not reachable anymore. Aborting file transfer.", device.getIp());
      showMessageDialog("Error", "El dispositivo se ha desconectado", ERROR);
      return;
    }

    Thread.startVirtualThread(() -> {
      try {
        long fileSize = file.length();
        long startTime = System.currentTimeMillis();

        long sent = 0;
        var elapsedTime = "00:00s";

        while (sent < fileSize) {
          Thread.sleep(200);
          long chunk = Math.min(1024 * 1024, fileSize - sent);
          sent += chunk;
          double progress = (double) sent / fileSize;

          if (fileTransfer.isCancelled()) {
            log.info("### File transfer cancelled");
            fileTransfer.setStatus(FileTransfer.Status.FAILED);
            fileTransfer.setErrorMessage("Transferencia cancelada");
            fileTransfer.setErrorDetails("El usuario canceló la transferencia");

            updateFileCard(
                fileCard,
                progress,
                0,
                elapsedTime,
                FileTransfer.Status.FAILED,
                () -> {
                  showMessageDialog(
                      "Transferencia cancelada",
                      "El usuario canceló la transferencia",
                      ERROR
                  );
                }
            );

            return;
          }

          long elapsedMillis = System.currentTimeMillis() - startTime;
          double elapsedSeconds = Math.max(elapsedMillis / 1000.0, 0.1);
          double speedMbPerSec = (sent / 1024.0 / 1024.0) / elapsedSeconds;
          long elapsed = elapsedMillis / 1000;
          elapsedTime = String.format("%02d:%02ds", elapsed / 60, elapsed % 60);

          fileTransfer.setProgress(progress);
          fileTransfer.setSpeed(speedMbPerSec);
          fileTransfer.setElapsedTime(elapsedTime);
          fileTransfer.setStatus(FileTransfer.Status.IN_PROGRESS);

          updateFileCard(
              fileCard,
              progress,
              speedMbPerSec,
              elapsedTime,
              FileTransfer.Status.IN_PROGRESS,
              () -> {
                log.info("### Cancelling file transfer...");
                fileTransfer.setStatus(FileTransfer.Status.CANCELLED);
                fileTransfer.setCancelled(true);
              }
          );
        }

        fileSender.sendFile(device.getIp(), file, fileTransfer);

        fileTransfer.setProgress(1.0);
        fileTransfer.setSpeed(0.0);
        fileTransfer.setStatus(FileTransfer.Status.SUCCESS);

        updateFileCard(
            fileCard,
            1.0,
            0,
            elapsedTime,
            FileTransfer.Status.SUCCESS,
            () -> {
              log.info("### File transfer completed successfully");
            }
        );

        log.info("### File successfully sent");

      } catch (Exception ex) {
        log.error("### Error sending file: {}", ex.getMessage(), ex);

        fileTransfer.setStatus(FileTransfer.Status.FAILED);

        updateFileCard(
            fileCard,
            1.0,
            0,
            "00:00s",
            FileTransfer.Status.FAILED,
            () -> {
              log.error("### Error updating file card: {}", ex.getMessage(), ex);
              showMessageDialog("Error", "Error al enviar el archivo", ERROR);
            }
        );
      }
    });
  }

  private boolean isDeviceStillOnline(Device device) {
    boolean existsInUI = vboxConnectedDevices.getChildren().stream()
        .filter(node -> node instanceof HBox)
        .map(node -> (HBox) node)
        .map(hbox -> (Device) hbox.getProperties().get("device"))
        .filter(Objects::nonNull)
        .anyMatch(d -> d.getIp().equals(device.getIp()));

    if (!existsInUI) {
      return false;
    }

    return deviceNetwork.canOpenConnection(device);
  }

  @FXML
  private void handleRefreshDevices() {
    log.info("### Refreshing connected devices...");
    btnRefresh.setDisable(true);

    var rotate = new RotateTransition(Duration.seconds(1), refreshIconContainer);
    rotate.setByAngle(360);
    rotate.setCycleCount(RotateTransition.INDEFINITE);
    rotate.setInterpolator(Interpolator.LINEAR);
    rotate.play();

    Thread.startVirtualThread(() -> {
      try {
        lanDiscoveryListener.removeExpiredDevicesManual();
        var connectedDevices = lanDiscoveryListener.getConnectedDevices();

        // Add devices that are not in the UI
        Platform.runLater(() -> {
          vboxConnectedDevices.getChildren().removeIf(node -> {
            if (!(node instanceof HBox)) {
              return false;
            }
            var device = (Device) node.getProperties().get("device");
            var key = device.getName() + "|" + device.getIp();
            return !connectedDevices.containsKey(key);
          });

          // Add devices that are in the UI but not in the connectedDevices map
          for (var entry : connectedDevices.entrySet()) {
            var entryKey = entry.getKey();
            var device = entry.getValue();
            var key = device.getName() + "|" + device.getIp();

            var exists = vboxConnectedDevices.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> (HBox) node)
                .map(hbox -> (Device) hbox.getProperties().get("device"))
                .anyMatch(d -> d != null && key.equals(entryKey));

            if (!exists) {
              var deviceBox = createDeviceBox(device, this::selectDevice);
              vboxConnectedDevices.getChildren().add(deviceBox);
            }
          }

          // Stop animation
          rotate.stop();
          refreshIconContainer.setRotate(0);
          btnRefresh.setDisable(false);

          log.info("### Found {} devices connected", lanDiscoveryListener.getConnectedDevices().size());
        });
      } catch (Exception e) {
        log.error("### Error refreshing devices: {}", e.getMessage(), e);

        Platform.runLater(() -> {
          rotate.stop();
          refreshIconContainer.setRotate(0);
          btnRefresh.setDisable(false);

          showMessageDialog("Error", "Error al actualizar los dispositivos", ERROR);
        });
      }
    });
  }
}
