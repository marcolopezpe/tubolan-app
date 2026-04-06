package pe.marcolopez.apps.tubolan.runneables;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pe.marcolopez.apps.tubolan.utils.FilesUtil;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static pe.marcolopez.apps.tubolan.utils.ConstantsUtil.DEFAULT_SERVER_SOCKET_PORT;

@Slf4j
@Singleton
public class FileReceiverServer implements Runnable {

  @ConfigProperty(name = "application.server.socket.port", defaultValue = DEFAULT_SERVER_SOCKET_PORT)
  int portServerSocket;

  @Override
  public void run() {
    try (ServerSocket serverSocket = new ServerSocket(portServerSocket)) {
      log.info("### Waiting for files on port {}...", portServerSocket);
      while (true) {
        var client = serverSocket.accept();
        Thread.startVirtualThread(() -> handleClient(client));
      }
    } catch (Exception e) {
      log.error("### Error to start server: {}", e.getMessage(), e);
    }
  }

  private void handleClient(Socket client) {
    try (client; var dis = new DataInputStream(client.getInputStream())) {
      try {
        var fileName = dis.readUTF();
        var fileSize = dis.readLong();
        var file = new File(FilesUtil.getDefaultDownloadFolder(), fileName);
        file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
          var buffer = new byte[4096];
          var remaining = fileSize;
          int read;
          while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
            fos.write(buffer, 0, read);
            remaining -= read;
          }
        }

        log.info("### File received: {} size: {} bytes", file.getName(), fileSize);
      } catch (Exception e) {
        log.error("### Error receiving file on handleClient: {}", e.getMessage(), e);
      }
    } catch (IOException ignored) {
    }
  }

  public void startServer() {
    Thread.startVirtualThread(this);
  }
}
