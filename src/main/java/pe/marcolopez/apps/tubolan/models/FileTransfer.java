package pe.marcolopez.apps.tubolan.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.File;

@Data
@AllArgsConstructor
public class FileTransfer {

  private String errorMessage;
  private String errorDetails;
  private Device targetDevice;
  private File file;
  private Double progress; // 0.0 a 1.0
  private Double speed; // MB/s
  private String elapsedTime;
  private Status status;
  private boolean isCancelled;

  public FileTransfer(File file) {
    this.file = file;
    this.progress = 0.0;
    this.speed = 0.0;
    this.elapsedTime = "00:00s";
    this.status = Status.IN_PROGRESS;
    this.errorMessage = "";
    this.errorDetails = "";
  }

  public enum Status {
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELLED
  }
}
