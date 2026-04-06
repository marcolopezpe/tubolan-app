package pe.marcolopez.apps.tubolan.config;

import jakarta.inject.Singleton;
import lombok.Data;
import pe.marcolopez.apps.tubolan.models.Device;

@Data
@Singleton
public class DeviceSession {

  private Device device;
  private boolean darkMode;
}
