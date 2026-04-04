package pe.marcolopez.apps.tubolan.runneables;

import pe.marcolopez.apps.tubolan.views.HomeController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LanDiscoveryListener implements Runnable {

  private static final int PORT = 7549;
  private final HomeController homeController;

  public LanDiscoveryListener(HomeController homeController) {
    this.homeController = homeController;
  }

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket(PORT)) {
      byte[] buffer = new byte[1024];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());
        if (message.contains("Tubolan")) {
          String[] parts = message.split("\\|");
          if (parts.length >= 3) {
            String deviceName = parts[1];
            String deviceIp = parts[2];

            homeController.addConnectedDevice(deviceName, deviceIp);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void start(HomeController controller) {
    Thread.startVirtualThread(new LanDiscoveryListener(controller));
  }
}
