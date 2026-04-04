package pe.marcolopez.apps.tubolan.runneables;

import pe.marcolopez.apps.tubolan.utils.DeviceInfoUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

      InetAddress broadcast = DeviceInfoUtil.getBroadcastAddress();
      DatagramPacket packet = new DatagramPacket(data, data.length, broadcast, PORT);

      socket.send(packet);

      while (true) {
        socket.send(packet);
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
