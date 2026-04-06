package pe.marcolopez.apps.tubolan.utils;

import lombok.Getter;
import java.io.File;

public class FilesUtil {

  @Getter
  private static File downloadFolder = getDefaultDownloadFolder();

  public static File getDefaultDownloadFolder() {
    if (downloadFolder != null) {
      return downloadFolder;
    }

    var home = System.getProperty("user.home");
    var desktop = new File(home, "Desktop");

    if (!desktop.exists()) {
      desktop.mkdirs();
    }

    return desktop;
  }

  public static void setDownloadFolder(File folder) {
    if (folder != null && folder.exists() && folder.isDirectory()) {
      downloadFolder = folder;
    }
  }
}
