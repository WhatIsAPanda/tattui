package app.controller;

import app.entity.LoggedInProfile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskbarController {

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());
    @FXML
    private Circle settingsProfileImage;

    private static TaskbarController instance;

    public static void setInstance(TaskbarController instance) {
        TaskbarController.instance = instance;
    }

    public static TaskbarController getInstance() {
        return instance;
    }

    public void setProfileImage(Image image) {
        ImagePattern pfpImagePattern = new ImagePattern(image);
        settingsProfileImage.setFill(pfpImagePattern);
    }
    public void showProfileImage(boolean showProfileImage) {
        settingsProfileImage.setVisible(showProfileImage);
    }
    @FXML
    private void handleClick(ActionEvent e) {
        Object src = e.getSource();

        if (!(src instanceof Button b)) return;

        RootController ctrl = RootController.getInstance();
        switch (b.getId()) {
            case "workspaceButton" -> ctrl.showPage("workspace");
            case "exploreButton" -> ctrl.showPage("explore");
            case "mapButton" -> ctrl.showPage("map");
            case "loginButton" -> ctrl.showPage("login");
            case "settingsButton" -> ctrl.showPage("viewProfile", LoggedInProfile.getInstance());
            case "search" -> {

            }
            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }
        
    }
}
