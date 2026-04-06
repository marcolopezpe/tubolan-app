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

      while (true) {
        var packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        var message = new String(packet.getData(), 0, packet.getLength());
        if (message.contains(APP_NAME)) {
          var parts = message.split("\\|");
          if (parts.length >= 3) {
            var deviceName = parts[1];
            var deviceIp = parts[2];
            this.addDevice(deviceName, deviceIp);
            this.removeExpiredDevicesAuto();
          }
        }
      }
    } catch (Exception e) {
      log.error("### Error in LanDiscoveryListener: {}", e.getMessage(), e);
    }
  }

  private void addDevice(String deviceName, String deviceIp) {
    if (deviceIp.equals(ip)) {
      return;
    }

    var key = deviceName + "|" + deviceIp;
    var now = System.currentTimeMillis();
    lastSeenMap.put(key, now);

    connectedDevices.computeIfAbsent(key, k -> {
      var device = new Device(deviceName, deviceIp, true);
      log.info("### New device discovered: {}", device);
      return device;
    });
  }

  public void removeExpiredDevicesAuto() {
    var now = System.currentTimeMillis();

    connectedDevices.keySet().removeIf(key -> {
      var lastSeen = lastSeenMap.get(key);
      var expired = lastSeen == null || now - lastSeen > DEFAULT_TIMEOUT_EXPIRED_AUTO;
      if (expired) {
        log.info("### Removing expired device: {}", key);
        lastSeenMap.remove(key);
      }
      return expired;
    });
  }

  public void removeExpiredDevicesManual() {
    long now = System.currentTimeMillis();

    try {
      Thread.sleep(DEFAULT_TIMEOUT_EXPIRED_MANUAL);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    connectedDevices.keySet().removeIf(key -> {
      var lastSeen = lastSeenMap.get(key);
      var expired = lastSeen == null || now - lastSeen >= DEFAULT_TIMEOUT_EXPIRED_MANUAL;
      if (expired) {
        lastSeenMap.remove(key);
        log.info("### Removing offline device immediately: {}", key);
      }
      return expired;
    });
  }

  public void start() {
    Thread.startVirtualThread(this);
    Thread.startVirtualThread(this::removeExpiredDevicesAuto);
  }
}
