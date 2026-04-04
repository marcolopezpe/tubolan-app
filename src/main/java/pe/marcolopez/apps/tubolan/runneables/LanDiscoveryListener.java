package pe.marcolopez.apps.tubolan.runneables;

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;
import pe.marcolopez.apps.tubolan.views.HomeController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LanDiscoveryListener implements Runnable {

  private static final int PORT = 7549;

  private final HomeController homeController;
  private final Map<String, HBox> connectedDevices = new ConcurrentHashMap();
  private final Map<String, Long> lastSeenMap = new ConcurrentHashMap();

  public LanDiscoveryListener(HomeController homeController) {
    this.homeController = homeController;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket(PORT)) {
      byte[] buffer = new byte[1024];

      Thread.startVirtualThread(this::removeExpiredDevices);

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());
        if (message.contains("Tubolan")) {
          String[] parts = message.split("\\|");
          if (parts.length >= 3) {
            String deviceName = parts[1];
            String deviceIp = parts[2];

            if (deviceIp.equals(DeviceInfoUtil.getRealLocalIp())) {
              continue;
            }

            String key = deviceName + "|" + deviceIp;
            lastSeenMap.put(key, System.currentTimeMillis());

            if (!connectedDevices.containsKey(key)) {
              homeController.addConnectedDevice(deviceName, deviceIp,hbox ->
                  connectedDevices.put(key, hbox));
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void removeExpiredDevices() {
    while (true) {
      long now = System.currentTimeMillis();

      lastSeenMap.forEach((key, lastSeen) -> {
        if (now - lastSeen > 5_000) {
          HBox hbox = connectedDevices.remove(key);
          if (hbox != null) {
            Platform.runLater(() -> homeController.removeConnectedDevice(hbox));
          }
          lastSeenMap.remove(key);
        }
      });

      try {
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public static void start(HomeController controller) {
    Thread.startVirtualThread(new LanDiscoveryListener(controller));
  }
}
