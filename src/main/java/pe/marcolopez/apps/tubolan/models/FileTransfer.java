package pe.marcolopez.apps.tubolan.models;

import java.io.File;

public class FileTransfer {

  public enum Status {
    IN_PROGRESS,
    SUCCESS,
    FAILED
  }

  private File file;
  private Double progress; // 0.0 a 1.0
  private Double speed; // MB/s
  private String elapsedTime;
  private Status status;

  public FileTransfer(File file) {
    this.file = file;
    this.progress = 0.0;
    this.speed = 0.0;
    this.elapsedTime = "00:00s";
    this.status = Status.IN_PROGRESS;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public Double getProgress() {
    return progress;
  }

  public void setProgress(Double progress) {
    this.progress = progress;
  }

  public Double getSpeed() {
    return speed;
  }

  public void setSpeed(Double speed) {
    this.speed = speed;
  }

  public String getElapsedTime() {
    return elapsedTime;
  }

  public void setElapsedTime(String elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
