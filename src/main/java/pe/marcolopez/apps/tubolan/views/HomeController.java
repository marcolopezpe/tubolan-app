package pe.marcolopez.apps.tubolan.views;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.Dependent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import pe.marcolopez.apps.tubolan.models.Device;
import pe.marcolopez.apps.tubolan.models.FileTransfer;
import pe.marcolopez.apps.tubolan.networks.FileSender;
import pe.marcolopez.apps.tubolan.runneables.FileReceiverServer;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryBroadcaster;
import pe.marcolopez.apps.tubolan.runneables.LanDiscoveryListener;
import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;

import java.awt.*;
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

  @FXML
  VBox vboxFileTransfers;

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
    if (device == null) {
      IO.println("No hay dispositivo seleccionado");
      return;
    }

    IO.println("### Sending file to " + device.getName() + " (" + device.getIp() + ")...");

    FileTransfer fileTransfer = new FileTransfer(file);
    fileTransfer.setStatus(FileTransfer.Status.IN_PROGRESS);

    HBox fileCard = createFileCard(fileTransfer);

    Platform.runLater(() -> vboxFileTransfers.getChildren().addFirst(fileCard));

    IO.println("### File card created: " + fileCard);

    Thread.startVirtualThread(() -> {
      try {
        long fileSize = file.length();
        long startTime = System.currentTimeMillis();

        long sent = 0;
        String elapsedTime = "00:00s";

        while (sent < fileSize) {
          Thread.sleep(200);

          long chunk = Math.min(1024 * 1024, fileSize - sent);
          sent += chunk;

          double progress = (double) sent / fileSize;

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
              FileTransfer.Status.IN_PROGRESS
          );
        }

        FileSender.sendFile(device.getIp(), file);

        fileTransfer.setProgress(1.0);
        fileTransfer.setSpeed(0.0);
        fileTransfer.setStatus(FileTransfer.Status.SUCCESS);

        updateFileCard(
            fileCard,
            1.0,
            0,
            elapsedTime,
            FileTransfer.Status.SUCCESS
        );

        IO.println("### File successfully sent");

      } catch (Exception ex) {
        ex.printStackTrace();

        fileTransfer.setStatus(FileTransfer.Status.FAILED);

        updateFileCard(
            fileCard,
            0,
            0,
            "00:00s",
            FileTransfer.Status.FAILED
        );

        IO.println("### Error sending file: " + ex.getMessage());
      }
    });
  }

  public HBox createFileCard(FileTransfer fileTransfer) {
    HBox hbox = new HBox(20);
    hbox.getStyleClass().add("file-card");
    hbox.setAlignment(Pos.BOTTOM_LEFT);

    Label lblIcon = new Label();
    lblIcon.getStyleClass().add("file-icon");
    FontIcon icon = new FontIcon(getIconForFile(fileTransfer.getFile()));
    lblIcon.setGraphic(icon);

    VBox vboxInfo = new VBox(5);
    vboxInfo.setMaxWidth(Double.MAX_VALUE);

    Label lblName = new Label(fileTransfer.getFile().getName());
    lblName.getStyleClass().add("file-name");

    HBox hboxStats = new HBox(5);
    hboxStats.setAlignment(Pos.CENTER);

    Label lblSize = new Label(humanReadableByteCount(fileTransfer.getFile().length(), true));
    lblSize.getStyleClass().add("file-size");

    Region spacer1 = new Region();
    HBox.setHgrow(spacer1, Priority.ALWAYS);

    Label lblSpeed = new Label("0 MB/s");
    lblSpeed.getStyleClass().add("file-speed");

    Region spacer2 = new Region();
    HBox.setHgrow(spacer2, Priority.ALWAYS);

    Label lblTime = new Label("00:00s");
    lblTime.getStyleClass().add("file-time");

    hboxStats.getChildren().addAll(lblSize, spacer1, lblSpeed, spacer2, lblTime);

    ProgressBar progressBar = new ProgressBar(0);
    progressBar.getStyleClass().add("progress-bar");

    vboxInfo.getChildren().addAll(lblName, hboxStats, progressBar);

    VBox vboxButton = new VBox();
    vboxButton.setAlignment(Pos.BOTTOM_RIGHT);
    Button btnAction = new Button("Cancelar");
    btnAction.getStyleClass().add("card-button");
    vboxButton.getChildren().add(btnAction);

    hbox.getChildren().addAll(lblIcon, vboxInfo, vboxButton);

    hbox.getProperties().put("transfer", fileTransfer);
    hbox.getProperties().put("progressBar", progressBar);
    hbox.getProperties().put("lblSpeed", lblSpeed);
    hbox.getProperties().put("lblTime", lblTime);
    hbox.getProperties().put("btnAction", btnAction);

    btnAction.setOnAction(e -> handleFileAction(hbox));

    return hbox;
  }

  private String getIconForFile(File file) {
    String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
    return switch (extension) {
      case "pdf" -> "fas-file-pdf";
      case "doc", "docx" -> "fas-file-word";
      case "xls", "xlsx" -> "fas-file-excel";
      case "zip", "rar", "7z" -> "fas-file-archive";
      case "png", "jpg", "jpeg", "gif", "bmp", "webp" -> "fas-file-image";
      case "mp4", "avi", "mkv", "mov" -> "fas-file-video";
      case "mp3", "wav", "ogg" -> "fas-file-audio";
      case "txt" -> "fas-file-alt";
      case "csv" -> "fas-file-csv";
      case "ppt", "pptx" -> "fas-file-powerpoint";
      case "java", "js", "ts", "html", "css", "xml", "json", "yml", "yaml" -> "fas-file-code";
      default -> "fas-file";
    };
  }

  private String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;

    if (bytes < unit) {
      return bytes + " B";
    }

    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String prefix = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");

    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), prefix);
  }

  private void handleFileAction(HBox hbox) {
    FileTransfer transfer = (FileTransfer) hbox.getProperties().get("transfer");
    Button btnAction = (Button) hbox.getProperties().get("btnAction");

    if (transfer == null || btnAction == null) {
      return;
    }

    IO.println("### File transfer: " + transfer.getFile().getName() + " - Status: " + transfer.getStatus());

    switch (transfer.getStatus()) {
      case IN_PROGRESS -> {
        btnAction.setDisable(true);
        btnAction.setText("Cancelando...");
      }

      case SUCCESS -> {
        try {
          File parentFolder = transfer.getFile().getParentFile();

          if (parentFolder != null && parentFolder.exists()) {
            Desktop.getDesktop().open(parentFolder);
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }

      case FAILED -> {
        btnAction.setDisable(true);
        btnAction.setText("Reintentando...");

        new Thread(() -> {
          try {
            transfer.setStatus(FileTransfer.Status.IN_PROGRESS);

            Platform.runLater(() -> {
              btnAction.setDisable(false);
              btnAction.setText("Cancelar");
            });

            Device device = (Device) selectedDeviceBox.getProperties().get("device");
            if (device != null) {
              FileSender.sendFile(device.getIp(), transfer.getFile());
            }
          } catch (Exception ex) {
            ex.printStackTrace();

            Platform.runLater(() -> {
              transfer.setStatus(FileTransfer.Status.FAILED);
              btnAction.setDisable(false);
              btnAction.setText("Reintentar");
            });
          }
        }).start();
      }
    }
  }

  public void updateFileCard(HBox hbox, double progress, double speed, String elapsedTime, FileTransfer.Status status) {
    Platform.runLater(() -> {
      ProgressBar pb = (ProgressBar) hbox.getProperties().get("progressBar");
      Label lblSpeed = (Label) hbox.getProperties().get("lblSpeed");
      Label lblTime = (Label) hbox.getProperties().get("lblTime");
      Button btnAction = (Button) hbox.getProperties().get("btnAction");
      FileTransfer transfer = (FileTransfer) hbox.getProperties().get("transfer");

      if (transfer != null) {
        transfer.setProgress(progress);
        transfer.setSpeed(speed);
        transfer.setElapsedTime(elapsedTime);
        transfer.setStatus(status);
      }

      pb.setProgress(progress);
      lblSpeed.setText(progress >= 1.0 ? "Completado" : String.format("%.1f MB/s", speed));
      lblTime.setText(elapsedTime);

      if (progress >= 1.0) {
        pb.getStyleClass().remove("progress-bar-success");
        pb.getStyleClass().add("progress-bar-success");
      }

      switch (status) {
        case IN_PROGRESS -> {
          btnAction.setDisable(false);
          btnAction.setText("Cancelar");
        }
        case SUCCESS -> {
          btnAction.setDisable(false);
          btnAction.setText("Abrir carpeta");
        }
        case FAILED -> {
          btnAction.setDisable(false);
          btnAction.setText("Reintentar");
        }
      }
    });
  }
}
