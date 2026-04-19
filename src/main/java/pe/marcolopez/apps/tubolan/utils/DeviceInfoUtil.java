package pe.marcolopez.apps.tubolan.utils;

import java.net.*;
import java.util.Enumeration;

public class DeviceInfoUtil {

  public static String getDeviceName() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      return localhost.getHostName();
    } catch (Exception e) {
      return "Unknown";
    }
  }

  public static String getLocalIp() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      return localhost.getHostAddress();
    } catch (Exception e) {
      return "0.0.0.0";
    }
  }

  public static String getRealLocalIp() {
    // First try: connect a UDP socket to an external IP and see which local IP is used
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
      String ip = socket.getLocalAddress().getHostAddress();
      if (ip != null && !ip.equals("0.0.0.0")) {
        return ip;
      }
    } catch (Exception ignored) {}

    // Second try: enumerate network interfaces and look for a private IPv4 address
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual())
          continue;

        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
          InetAddress addr = ia.getAddress();
          if (addr instanceof Inet4Address) {
            String ip = addr.getHostAddress();
            // RFC 1918 private ranges
            if (ip.startsWith("192.168.") || ip.startsWith("10.") ||
                ip.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")) {
              return ip;
            }
          }
        }
      }
    } catch (SocketException ignored) {}

    return "127.0.0.1";
  }
}
