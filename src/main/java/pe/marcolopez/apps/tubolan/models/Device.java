package pe.marcolopez.apps.tubolan.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Device {

  private String name;
  private String ip;
  private boolean online;

  public String toDisplayFull() {
    return name + " (" + ip + ")";
  }
}
