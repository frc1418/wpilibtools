package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Takes the extracted JRE and turns it into a customized JRE for the roboRio
 */
public class CreateJreController {

    private static String[] JRE_CREATE_COMMAND = {"java",
            "-jar",
            "",
            "--dest", "JRE",
            "--profile", "compact2",
            "--vm", "client",
            "--keep-debug-info",
            "--debug"
    };

    @FXML
    private BorderPane mainView;
    @FXML
    private Label commandLabel;

    private String m_untarredLocation;
    private String m_tarLocation;
    private Thread m_JRECreateThread;
    private final Logger m_logger = LogManager.getLogger();

    public void initialize(String untarredLocation, String tarLocation) {
        m_untarredLocation = untarredLocation;
        m_tarLocation = tarLocation;
        m_JRECreateThread = new Thread(() -> {
            final String jreCreateLibLocation = m_untarredLocation + File.separator + "lib" + File.separator + "JRECreate.jar";
            JRE_CREATE_COMMAND[2] = jreCreateLibLocation;
            m_logger.debug("Staring JRE Creation");
            m_logger.debug("Creator location: " + jreCreateLibLocation);
            m_logger.debug("Command: " + Arrays.toString(JRE_CREATE_COMMAND));
            Platform.runLater(() -> commandLabel.setText(Arrays.toString(JRE_CREATE_COMMAND)));
            try {
                // If the JRE folder already exists the create process will fail, so delete the folder if it exists
                final String jreLocation = new File(m_untarredLocation).getParent() + File.separator + "JRE";
                File jreFolder = new File(jreLocation);
                if (jreFolder.exists()) {
                    deleteFolder(jreFolder);
                }

                // Run the JRE create Process
                Process proc = Runtime.getRuntime().exec(
                        JRE_CREATE_COMMAND,
                        new String[]{
                                "EJDK_HOME=" + m_untarredLocation
                        },
                        new File(m_untarredLocation).getParentFile());
                if (proc.waitFor() != 0) {
                    String line = "";
                    StringBuilder output = new StringBuilder();
                    m_logger.error("JRE Creation failed, starting output");
                    m_logger.error("JRE Creation stdOut follows:");
                    // Echo the output from stdout and err
                    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                    m_logger.error(output.toString());
                    m_logger.error("JRE Creation stdOut end");
                    m_logger.error("JRE Creation stdErr start");
                    output = new StringBuilder();
                    reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append(System.lineSeparator());
                    }
                    m_logger.error(output.toString());

                    Platform.runLater(() -> MainApp.showErrorScreen(new Exception("JRE Creation failed, exit code " + proc.exitValue())));
                } else {
                    m_logger.debug("Successfully Created JRE!");
                    Platform.runLater(() -> {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/connect_roborio.fxml"));
                        try {
                            Parent root = loader.load();
                            ConnectRoboRioController controller = loader.getController();
                            controller.initialize(jreLocation, m_tarLocation, m_untarredLocation);
                            mainView.getScene().setRoot(root);
                        } catch (IOException e) {
                            m_logger.error("Could not load RoboRioConnect controller", e);
                            MainApp.showErrorScreen(e);
                        }
                    });
                }
            } catch (IOException | InterruptedException e) {
                m_logger.error("Could not create the custom JRE!", e);
                Platform.runLater(() -> MainApp.showErrorScreen(e));
            }
        });
        m_JRECreateThread.setDaemon(true);
        m_JRECreateThread.start();
    }

    @FXML
    public void handleBack(ActionEvent event) {
        m_JRECreateThread.interrupt();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/downloaded.fxml"));
        try {
            Parent root = loader.load();
            DownloadedController controller = loader.getController();
            controller.initialize(m_tarLocation);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not display downloaded page", e);
            MainApp.showErrorScreen(e);
        }
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        MainApp.showExitPopup();
    }

    /**
     * Recursively deletes a folder and all subfolders
     *
     * @param obj The directory to delete
     */
    private void deleteFolder(File obj) {
        if (obj.isFile()) {
            obj.delete();
        } else {
            for (File file : obj.listFiles()) {
                deleteFolder(file);
            }
            obj.delete();
        }
    }
}
