package pe.marcolopez.apps.tubolan.runneables;

import jakarta.inject.Singleton;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.*;

import static pe.marcolopez.apps.tubolan.utils.ConstantsUtil.*;

@Slf4j
@Singleton
public class LanDiscoveryBroadcaster implements Runnable {

  @ConfigProperty(name = "application.server.data.port", defaultValue = DEFAULT_SERVER_DATA_PORT)
  int portServerData;

  @Setter
  String deviceName;
  @Setter
  String deviceIp;

  @Override
  public void run() {
    try (var socket = new DatagramSocket()) {
      socket.setBroadcast(true);

      var message = APP_NAME + "|" + deviceName + "|" + deviceIp;
      var data = message.getBytes();

      while (true) {
        var interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
          var networkInterface = interfaces.nextElement();
          if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

          for (var ia : networkInterface.getInterfaceAddresses()) {
            var broadcast = ia.getBroadcast();
            if (broadcast != null) {
              var packet = new DatagramPacket(data, data.length, broadcast, portServerData);
              try {
                socket.send(packet);
              } catch (Exception e) {
                log.error("### Error sending broadcast packet: {}", e.getMessage(), e);
              }
            }
          }
        }

        Thread.sleep(DEFAULT_TIMEOUT_BROADCASTER);
      }
    } catch (Exception e) {
      log.error("### Error in LanDiscoveryBroadcaster: {}", e.getMessage(), e);
    }
  }

  public void start() {
    Thread.startVirtualThread(this);
  }
}
