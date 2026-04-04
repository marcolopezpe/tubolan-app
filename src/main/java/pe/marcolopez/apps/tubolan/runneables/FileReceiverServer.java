package pe.marcolopez.apps.tubolan.runneables;

import pe.marcolopez.apps.tubolan.utils.FileUtil;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiverServer implements Runnable {

  private static final int PORT = 5547;

  @Override
  public void run() {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      IO.println("Esperando archivos en puerto " + PORT);
      while (true) {
        Socket client = serverSocket.accept();
        Thread.startVirtualThread(() -> handleClient(client));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleClient(Socket client) {
    try (DataInputStream dis = new DataInputStream(client.getInputStream())) {
      String fileName = dis.readUTF();
      long fileSize = dis.readLong();
      File file = new File(FileUtil.getDefaultDownloadFolder(), fileName);
      file.getParentFile().mkdirs();

      try (FileOutputStream fos = new FileOutputStream(file)) {
        byte[] buffer = new byte[4096];
        long remaining = fileSize;
        int read;
        while (remaining > 0 && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
          fos.write(buffer, 0, read);
          remaining -= read;
        }
      }

      IO.println("Archivo recibido: " + file.getAbsolutePath());

    } catch (IOException e) {

    } finally {
      try {
        client.close();
      } catch (IOException ignored) {
      }
    }
  }

  public static void startServer() {
    Thread.startVirtualThread(new FileReceiverServer());
  }
}
