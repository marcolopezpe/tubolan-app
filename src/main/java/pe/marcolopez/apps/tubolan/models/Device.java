package pe.marcolopez.apps.tubolan.models;

public class Device {

  private String name;
  private String ip;
  private boolean online;

  public Device(String name, String ip, boolean online) {
    this.name = name;
    this.ip = ip;
    this.online = online;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public String toDiplayFull() {
    return name + " (" + ip + ")";
  }

  public String toDiplayShort() {
    return name;
  }
}
