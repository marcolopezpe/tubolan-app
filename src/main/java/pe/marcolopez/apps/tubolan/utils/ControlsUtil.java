package pe.marcolopez.apps.tubolan.utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import pe.marcolopez.apps.tubolan.models.Device;
import pe.marcolopez.apps.tubolan.models.FileTransfer;
import java.io.File;
import java.util.function.Consumer;

public class ControlsUtil {

  public static HBox createDeviceBox(Device device, Consumer<HBox> deviceBox) {
    var hbox = new HBox(10);
    hbox.setAlignment(Pos.CENTER_LEFT);
    hbox.getStyleClass().add("device-item");
    hbox.getProperties().put("device", device);

    var statusDot = new StackPane();
    statusDot.getStyleClass().addAll("status-dot-device", "status-online");

    var lblName = new Label(device.getName());
    lblName.getStyleClass().add("device-name");

    hbox.getChildren().addAll(statusDot, lblName);
    hbox.setOnMouseClicked(e -> deviceBox.accept(hbox));

    return hbox;
  }

  public static HBox createFileCard(FileTransfer fileTransfer) {
    var hbox = new HBox(20);
    hbox.getStyleClass().add("file-card");
    hbox.setAlignment(Pos.BOTTOM_LEFT);

    var lblIcon = new Label();
    lblIcon.getStyleClass().add("file-icon");
    // var icon = new FontIcon(getIconForFile(fileTransfer.getFile()));
    var icon = new SVGPath();
    icon.setContent(getSvgForFile(fileTransfer.getFile()));
    icon.getStyleClass().add("svg-icon");
    lblIcon.setGraphic(icon);

    var vboxInfo = new VBox(5);
    vboxInfo.setMaxWidth(Double.MAX_VALUE);

    var lblName = new Label(fileTransfer.getFile().getName());
    lblName.getStyleClass().add("file-name");

    var hboxStats = new HBox(5);
    hboxStats.setAlignment(Pos.CENTER);

    var lblSize = new Label(humanReadableByteCount(fileTransfer.getFile().length(), true));
    lblSize.getStyleClass().add("file-size");

    var spacer1 = new Region();
    HBox.setHgrow(spacer1, Priority.ALWAYS);

    var lblSpeed = new Label("0 MB/s");
    lblSpeed.getStyleClass().add("file-speed");

    var spacer2 = new Region();
    HBox.setHgrow(spacer2, Priority.ALWAYS);

    var lblTime = new Label("00:00s");
    lblTime.getStyleClass().add("file-time");

    hboxStats.getChildren().addAll(lblSize, spacer1, lblSpeed, spacer2, lblTime);

    var progressBar = new ProgressBar(0);
    progressBar.getStyleClass().add("progress-bar");

    vboxInfo.getChildren().addAll(lblName, hboxStats, progressBar);

    var vboxButton = new VBox();
    vboxButton.setAlignment(Pos.BOTTOM_RIGHT);
    var btnAction = new Button("Cancelar");
    btnAction.getStyleClass().add("card-button");
    vboxButton.getChildren().add(btnAction);

    hbox.getChildren().addAll(lblIcon, vboxInfo, vboxButton);

    hbox.getProperties().put("transfer", fileTransfer);
    hbox.getProperties().put("progressBar", progressBar);
    hbox.getProperties().put("lblSpeed", lblSpeed);
    hbox.getProperties().put("lblTime", lblTime);
    hbox.getProperties().put("btnAction", btnAction);
    hbox.getProperties().put("buttonAction", (Runnable) () -> {});

    btnAction.setOnAction(e -> {
      var action = (Runnable) hbox.getProperties().get("buttonAction");
      if (action != null) {
        action.run();
      }
    });

    return hbox;
  }

  public static void updateFileCard(HBox hbox, double progress, double speed, String elapsedTime, FileTransfer.Status status, Runnable onAction) {
    Platform.runLater(() -> {
      var pb = (ProgressBar) hbox.getProperties().get("progressBar");
      var lblSpeed = (Label) hbox.getProperties().get("lblSpeed");
      var lblTime = (Label) hbox.getProperties().get("lblTime");
      var btnAction = (Button) hbox.getProperties().get("btnAction");
      var transfer = (FileTransfer) hbox.getProperties().get("transfer");

      if (transfer != null) {
        transfer.setProgress(progress);
        transfer.setSpeed(speed);
        transfer.setElapsedTime(elapsedTime);
        transfer.setStatus(status);
      }

      pb.setProgress(progress);
      lblSpeed.setText(progress >= 1.0 ? "Completado" : String.format("%.1f MB/s", speed));
      lblTime.setText(elapsedTime);

      hbox.getProperties().put("buttonAction", onAction);

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
          btnAction.setDisable(true);
          btnAction.setText("OK");
        }
        case FAILED -> {
          pb.getStyleClass().add("progress-bar-error");
          assert transfer != null;
          lblSpeed.setText("Error: " + transfer.getErrorMessage());
          btnAction.setDisable(false);
          btnAction.setText("Ver detalles");
        }
      }
    });
  }

  public static void showMessageDialog(String title, String message, Alert.AlertType type) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private static String getSvgForFile(File file) {
    var ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();

    return switch (ext) {
      case "pdf" -> "M6 2h9l5 5v13a2 2 0 0 1-2 2H6z";
      case "doc", "docx" -> "M4 4h16v16H4z";
      case "xls", "xlsx" -> "M3 3h18v18H3z";
      case "png", "jpg", "jpeg" -> "M4 4h16v16H4z M8 14l2-2 3 3 4-4 3 5";
      case "mp4" -> "M4 4h16v16H4z M10 8l6 4-6 4z";
      default -> "M5 3h14v18H5z";
    };
  }

  private static String getIconForFile(File file) {
    var extension = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
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

  private static String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;

    if (bytes < unit) {
      return bytes + " B";
    }

    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String prefix = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");

    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), prefix);
  }
}
