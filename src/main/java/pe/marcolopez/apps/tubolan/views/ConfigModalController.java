package pe.marcolopez.apps.tubolan.views;

import io.quarkiverse.fx.views.FxView;
import jakarta.enterprise.context.Dependent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import pe.marcolopez.apps.tubolan.utils.FilesUtil;
import java.io.File;

@Slf4j
@FxView
@Dependent
public class ConfigModalController {

  @FXML
  @Getter
  StackPane root;

  @FXML
  TextField txtFolderField;

  @FXML
  Button btnBrowseButton;

  @Getter
  String selectedFolder;

  @FXML
  public void initialize() {
    log.info("### ConfigModalController initialized");
    txtFolderField.setEditable(false);

    if (selectedFolder != null) {
      txtFolderField.setText(selectedFolder);
    } else {
      var defaultFolder = FilesUtil.getDefaultDownloadFolder().getAbsolutePath();
      txtFolderField.setText(defaultFolder);
    }
  }

  @FXML
  private void handleBrowseFolder() {
    var chooser = new DirectoryChooser();
    var currentDir = new File(txtFolderField.getText());
    if (currentDir.exists()) {
      chooser.setInitialDirectory(currentDir);
    }

    var ownerWindow = root.getScene().getWindow();
    var selectedDir = chooser.showDialog(ownerWindow);
    if (selectedDir != null) {
      txtFolderField.setText(selectedDir.getAbsolutePath());
      log.info("### Selected folder: {}", selectedDir.getAbsolutePath());
    }
  }

  @FXML
  private void handleSaveFolder() {
    log.info("### Save clicked");
    var folderToSave = txtFolderField.getText();
    if (folderToSave != null && !folderToSave.isEmpty()) {
      selectedFolder = folderToSave;
      log.info("### Selected folder to save: {}", selectedFolder);

      FilesUtil.setDownloadFolder(new File(selectedFolder));
      log.info("### Download folder set to: {}", selectedFolder);

      var stage = (Stage) root.getScene().getWindow();
      stage.close();
    } else {
      log.error("### No folder selected");
    }
  }
}
