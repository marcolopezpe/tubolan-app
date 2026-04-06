package pe.marcolopez.apps.tubolan.networks;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pe.marcolopez.apps.tubolan.models.FileTransfer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

import static pe.marcolopez.apps.tubolan.utils.ConstantsUtil.DEFAULT_SERVER_SOCKET_PORT;

@Slf4j
@ApplicationScoped
public class FileSender {

  @ConfigProperty(name = "application.server.socket.port", defaultValue = DEFAULT_SERVER_SOCKET_PORT)
  int portServerSocket;

  public void sendFile(String destinationIp, File file, FileTransfer fileTransfer) {
    Thread.startVirtualThread(() -> {
      try (Socket socket = new Socket(destinationIp, portServerSocket);
           var dos = new DataOutputStream(socket.getOutputStream());
           var fis = new FileInputStream(file)) {

        dos.writeUTF(file.getName());
        dos.writeLong(file.length());

        var buffer = new byte[4096];
        int read;
        while ((read = fis.read(buffer)) != -1) {
          if (fileTransfer.isCancelled()) {
            throw new IOException("### File transfer cancelled");
          }
          dos.write(buffer, 0, read);
        }
        dos.flush();

        log.info("### File sent: {} size: {} bytes to {}", file.getName(), file.length(), destinationIp);
      } catch (IOException e) {
        if (fileTransfer.isCancelled()) {
          throw new RuntimeException("### File transfer cancelled");
        }
        log.error("### Error sending file: {} to {}: {}", file.getName(), destinationIp, e.getMessage());
      }
    });
  }
}
