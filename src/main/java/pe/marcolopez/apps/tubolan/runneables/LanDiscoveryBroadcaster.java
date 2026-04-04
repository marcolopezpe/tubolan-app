package pe.marcolopez.apps.tubolan.runneables;

import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;

import java.net.*;
import java.util.Enumeration;

public class LanDiscoveryBroadcaster implements Runnable {

  private static final int PORT = 7549;

  private final String deviceName;
  private final String deviceIp;

  public LanDiscoveryBroadcaster(String deviceName, String deviceIp) {
    this.deviceName = deviceName;
    this.deviceIp = deviceIp;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setBroadcast(true);

      String message = "Tubolan|" + deviceName + "|" + deviceIp;
      byte[] data = message.getBytes();

      while (true) {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
          NetworkInterface ni = interfaces.nextElement();
          if (ni.isLoopback() || !ni.isUp()) continue;

          for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
            InetAddress broadcast = ia.getBroadcast();
            if (broadcast != null) {
              DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, PORT);
              try {
                socket.send(packet);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        }

        Thread.sleep(3000);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void start(String deviceName, String deviceIp) {
    Thread.startVirtualThread(new LanDiscoveryBroadcaster(deviceName, deviceIp));
  }
}
