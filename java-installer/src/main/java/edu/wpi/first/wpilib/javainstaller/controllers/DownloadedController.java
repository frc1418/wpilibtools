package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import net.mightypork.rpack.utils.DesktopApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Alerts the user to being done the download. Also opens a window to the JRE download location if the user needs to move
 * computer
 */
public class DownloadedController extends AbstractControllerOld {

    @FXML
    private Label textView;

    @FXML
    private Button nextButton;

    private String m_path;
    private final Logger m_logger = LogManager.getLogger();

    public DownloadedController() {
        // The previous to this would be the progress controller, but we don't have a url for it, or cookies, so
        // send them back to the web page
        super("/fxml/download.fxml");
    }

    public void initialize(String path) {
        if (!MainApp.checkJreCreator(new File(path))) {
            textView.setText("An unknown error occurred when downloading the JRE, and the JRE is corrupt. To Redownload the JRE, hit back, and sign in again.");
            nextButton.setDisable(true);
        }
        m_path = path;
    }

    /**
     * Opens the JRE in the native file browser
     *
     * @param event unused
     */
    public void handleOpenDirectory(ActionEvent event) {
        File jreFile = new File(m_path);
        File parent = jreFile.getParentFile();
        DesktopApi.open(parent);
    }
}
