package pe.marcolopez.apps.tubolan.utils;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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

        String interfaceName = networkInterface.getDisplayName().toLowerCase();

        if (!networkInterface.isUp()
            || networkInterface.isLoopback()
            || interfaceName.contains("virtual")
            || interfaceName.contains("vmware")
            || interfaceName.contains("hyper-v")
            || interfaceName.contains("veth")
            || interfaceName.contains("docker")
            || interfaceName.contains("wsl")
            || interfaceName.contains("vethernet")
            || interfaceName.contains("bluetooth")) {
          continue;
        }

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();

          if (!address.isLoopbackAddress()
              && address instanceof java.net.Inet4Address) {

            String ip = address.getHostAddress();

            if (ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.startsWith("172.")) {
              return ip;
            }
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }

    return "0.0.0.0";
  }

  public static InetAddress getBroadcastAddress() throws SocketException {
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      if (networkInterface.isLoopback() || !networkInterface.isUp()) continue;

      for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
        InetAddress broadcast = interfaceAddress.getBroadcast();
        if (broadcast != null) {
          return broadcast;
        }
      }
    }

    try {
      return InetAddress.getByName("255.255.255.255");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
