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
        NetworkInterface ni = interfaces.nextElement();

        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

        String name = ni.getName().toLowerCase();

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        if (isMac && !(name.equals("en0") || name.equals("en1"))) continue;

        for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
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
