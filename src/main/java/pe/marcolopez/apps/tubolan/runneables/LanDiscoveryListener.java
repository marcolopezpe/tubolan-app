package pe.marcolopez.apps.tubolan.runneables;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LanDiscoveryListener implements Runnable {

  private static final int PORT = 7549;

  @Override
  public void run() {
    try (DatagramSocket socket = new DatagramSocket(PORT)) {
      byte[] buffer = new byte[1024];

      while (true) {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());
        if (message.contains("Tubolan")) {
          IO.println("Dispositivo encontrado: " + message);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void start() {
    Thread.startVirtualThread(new LanDiscoveryListener());
  }
}
