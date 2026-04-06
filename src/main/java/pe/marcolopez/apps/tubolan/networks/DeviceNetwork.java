package pe.marcolopez.apps.tubolan.networks;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pe.marcolopez.apps.tubolan.models.Device;
import pe.marcolopez.apps.tubolan.utils.ConstantsUtil;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

import static pe.marcolopez.apps.tubolan.utils.ConstantsUtil.DEFAULT_SERVER_SOCKET_PORT;

@Slf4j
@Singleton
public class DeviceNetwork {

  @ConfigProperty(name = "application.server.socket.port", defaultValue = DEFAULT_SERVER_SOCKET_PORT)
  int portServerSocket;

  private String getDeviceName() {
    try {
      InetAddress localhost = InetAddress.getLocalHost();
      return localhost.getHostName();
    } catch (Exception e) {
      return ConstantsUtil.DEFAULT_DEVICE_NAME;
    }
  }

  private String getRealLocalIp() {
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
      log.error("### Error getting real local IP: {}", e.getMessage(), e);
    }

    return ConstantsUtil.DEFAULT_IP;
  }

  public boolean canOpenConnection(Device device) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(device.getIp(), portServerSocket), 2000);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public Device getDeviceLocal() {
    Device device = new Device();
    device.setName(this.getDeviceName());
    device.setIp(this.getRealLocalIp());
    return device;
  }
}
