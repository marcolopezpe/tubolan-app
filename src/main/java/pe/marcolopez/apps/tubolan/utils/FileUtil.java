package pe.marcolopez.apps.tubolan.utils;

import java.io.File;

public class FileUtil {

  public static File getDefaultDownloadFolder() {
    String home = System.getProperty("user.home");
    File desktop = new File(home, "Desktop");

    if (!desktop.exists()) {
      desktop.mkdirs();
    }

    return desktop;
  }
}
