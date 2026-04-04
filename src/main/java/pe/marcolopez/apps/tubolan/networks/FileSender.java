package pe.marcolopez.apps.tubolan.networks;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class FileSender {

  public static void sendFile(String ipDestino, int port, File file) {
    Thread.startVirtualThread(() -> {
      try (Socket socket = new Socket(ipDestino, port);
           DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
           FileInputStream fis = new FileInputStream(file)) {

        dos.writeUTF(file.getName());
        dos.writeLong(file.length());

        byte[] buffer = new byte[4096];
        int read;
        while ((read = fis.read(buffer)) != -1) {
          dos.write(buffer, 0, read);
        }
        dos.flush();

        IO.println("Archivo enviado: " + file.getAbsolutePath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
