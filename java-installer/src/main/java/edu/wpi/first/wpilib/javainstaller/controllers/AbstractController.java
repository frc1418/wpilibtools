package edu.wpi.first.wpilib.javainstaller.controllers;

import edu.wpi.first.wpilib.javainstaller.Arguments;
import edu.wpi.first.wpilib.javainstaller.ControllerFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.io.IOException;

/**
 * Base controller abstraction that handles all navigation, cancellation button pressing, and the general logic. In
 * order for this controller to work, the fxml view for the concrete implementation must have a borderpane with the
 * name mainView. To handle the cancel button, the view must have a button that uses the {@link #handleCancel(javafx.event.ActionEvent)}
 * method as the onAction event. To handle back, The view must have a button that uses the {@link #handleBack(javafx.event.ActionEvent)}
 * method as the onAction event.
 */
public abstract class AbstractController {

    @FXML
    protected BorderPane mainView;
    protected final Arguments m_args = new Arguments();
    private final Logger m_logger = LogManager.getLogger();
    private final boolean m_addToBackStack;
    private final Arguments.Controller m_currentController;

    public AbstractController(boolean addToBackStack,
                              Arguments.Controller currentController) {
        m_addToBackStack = addToBackStack;
        m_currentController = currentController;
    }

    /**
     * Initializes this class with its list of intents. This takes care of the general setup, and then calls
     * {@link #initializeClass()} for class setup.
     *
     * @param args The arguments for this class
     */
    public void initialize(Arguments args) {
        m_args.copyFrom(args);
        initializeClass();
    }

    protected abstract void initializeClass();

    /**
     * Handles the cancel button being pressed.
     *
     * @param event The button event. Ignored
     */
    @FXML
    protected void handleCancel(ActionEvent event) {
        showExitPopup();
    }

    /**
     * Handles the back button being pressed. This uses the location provided by the concrete implementation to find
     * the correct back class and show it.
     *
     * @param event The button event. Ignored
     */
    @FXML
    protected void handleBack(ActionEvent event) {
        Arguments.Controller backController = m_args.popBackstack();
        moveWindow(backController, m_args);
    }

    /**
     * Helper method for moving to the next window. This view will be passed the current arguments instance from this
     * class. This method <b>MUST</b> be called from the main JavaFX thread.
     */
    protected void moveNext(Arguments.Controller nextController) {
        if (m_addToBackStack) {
            m_args.pushBackstack(m_currentController);
        }
        moveWindow(nextController, m_args);
    }

    /**
     * Helper method that takes care of the logic for moving from one screen to another.
     *
     * @param controller The controller to move to
     * @param args       The arguments to use when moving
     */
    private void moveWindow(Arguments.Controller controller, Arguments args) {
        m_logger.debug("Moving to controller " + controller);
        try {
            Parent root = ControllerFactory.getInstance().initializeController(controller, args);
            mainView.getScene().setRoot(root);
        } catch (IOException e) {
            m_logger.error("Could not initialize controller " + controller, e);
            showErrorScreen(e);
        }
    }

    /**
     * Shows a popup that exits the program on clicking yes.
     */
    @SuppressWarnings("deprecation")
    public void showExitPopup() {
        // TODO: When JDK8u40 is release (estimated March 2015) update this to official APIs
        m_logger.debug("Showing exit popup");
        Action action = Dialogs.create()
                .title("Exit")
                .message("Are you sure you want to quit? The roboRio will not be set up for Java until the installer has completed.")
                .actions(Dialog.ACTION_YES, Dialog.ACTION_NO)
                .showConfirm();

        if (action == Dialog.ACTION_YES) {
            m_logger.debug("Exiting installer from popup");
            Platform.exit();
        }
    }

    @SuppressWarnings("deprecation")
    public void showErrorPopup(String error, boolean exit) {
        Dialogs.create()
                .title("Error")
                .message(error)
                .showError();
        if (exit) {
            m_logger.debug("Exiting application from error popup");
            Platform.exit();
        }
    }

    @SuppressWarnings("deprecation")
    public void showErrorScreen(Exception e) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/error.fxml"));
        try {
            Parent root = loader.load();
            ErrorController controller = loader.getController();
            controller.initialize(e);
            mainView.getScene().setRoot(root);
        } catch (IOException ex) {
            LogManager.getLogger().error("Unknown error when displaying the logger", ex);
            Dialogs.create()
                    .title("Error Displaying Error")
                    .message("Something is really broken. Please copy the contents of the logs folder in " +
                            new File(".").getAbsolutePath() +
                            " to the FIRST discussion board")
                    .showError();
            Platform.exit();
        }
    }
}
