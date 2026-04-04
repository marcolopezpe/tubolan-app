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
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();

        if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;

        String name = networkInterface.getName();
        if (name.equals("en0")) { // Wi-Fi macOS
          for (InterfaceAddress ia : networkInterface.getInterfaceAddresses()) {
            InetAddress addr = ia.getAddress();
            if (addr instanceof Inet4Address) {
              return addr.getHostAddress();
            }
          }
        }

        for (InterfaceAddress ia : networkInterface.getInterfaceAddresses()) {
          InetAddress addr = ia.getAddress();
          if (addr instanceof Inet4Address) {
            String ip = addr.getHostAddress();
            if (ip.startsWith("192.") || ip.startsWith("10.")) {
              return ip;
            }
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }

    return "127.0.0.1";
  }
}
