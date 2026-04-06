package pe.marcolopez.apps.tubolan.runneables;

import jakarta.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pe.marcolopez.apps.tubolan.models.Device;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static pe.marcolopez.apps.tubolan.utils.ConstantsUtil.*;

@Slf4j
@Singleton
public class LanDiscoveryListener implements Runnable {

  @ConfigProperty(name = "application.server.data.port", defaultValue = DEFAULT_SERVER_DATA_PORT)
  int portServerData;

  @Getter
  ObservableMap<String, Device> connectedDevices = FXCollections.observableMap(new ConcurrentHashMap<>());
  Map<String, Long> lastSeenMap = new ConcurrentHashMap<>();
  @Setter
  String ip;

  @Override
  public void run() {
    try (var socket = new DatagramSocket(portServerData)) {
      var buffer = new byte[1024];

      Thread.startVirtualThread(this::removeExpiredDevices);

      while (true) {
        var packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        var message = new String(packet.getData(), 0, packet.getLength());
        if (message.contains(APP_NAME)) {
          var parts = message.split("\\|");
          if (parts.length >= 3) {
            var deviceName = parts[1];
            var deviceIp = parts[2];

            if (deviceIp.equals(ip)) {
              continue;
            }

            var key = deviceName + "|" + deviceIp;
            lastSeenMap.put(key, System.currentTimeMillis());

            if (!connectedDevices.containsKey(key)) {
              var device = new Device(deviceName, deviceIp, true);
              log.info("### Adding new device: {}", device.toDisplayFull());
              connectedDevices.put(key, device);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("### Error in LanDiscoveryListener: {}", e.getMessage(), e);
    }
  }

  private void removeExpiredDevices() {
    while (true) {
      var now = System.currentTimeMillis();

      lastSeenMap.forEach((key, lastSeen) -> {
        if (now - lastSeen > DEFAULT_TIMEOUT_LAST_SEEN) {
          log.info("### Removing expired device: {}", key);
          var device = connectedDevices.remove(key);
          if (device != null) {
            lastSeenMap.remove(key);
          }
        }
      });

      try {
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public void start() {
    Thread.startVirtualThread(this);
  }
}
