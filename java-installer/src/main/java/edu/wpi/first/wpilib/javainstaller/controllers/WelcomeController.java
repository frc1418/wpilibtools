package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import edu.wpi.first.wpilib.javainstaller.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;

/**
 * Shows the welcome screen
 */
public class WelcomeController extends AbstractController{

    @FXML
    private ImageView logoImageView;

    public WelcomeController() {
        super(true, Arguments.Controller.WELCOME_CONTROLLER, Arguments.Controller.INTERNET_CONTROLLER);
    }

    @Override
    protected void initializeClass() {
        Image frcImage = new Image(getClass().getResourceAsStream("/images/FRCicon_RGB.jpg"));
        logoImageView.setImage(frcImage);
        logoImageView.setPreserveRatio(true);
    }

    public void handleNext(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/internet.fxml"));
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            LogManager.getLogger().debug("Error when displaying connect internet screen", e);
            MainApp.showErrorScreen(e);
        }
    }
}
