package app.controller;

import app.entity.LoggedInProfile;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskbarController {

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());
    @FXML
    private Circle settingsProfileImage;
    @FXML
    private AnchorPane searchTab;
    private boolean searchTabIsOpen = false;
    private final TranslateTransition moveSearchTabAnimation = new TranslateTransition();
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
            case "mapButton" -> ctrl.showPage("map");
            case "loginButton" -> ctrl.showPage("login");
            case "settingsButton" -> ctrl.showPage("viewProfile", LoggedInProfile.getInstance());
            case "searchButton" -> {
                searchButtonClick();
            }
            case "exploreButton" -> ctrl.showPage("explore");
            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }
    }

    private void searchButtonClick() {
        if(moveSearchTabAnimation.getStatus() == Animation.Status.RUNNING) {
            moveSearchTabAnimation.stop();
        }
        double closedX = searchTab.getLayoutX();
        double openX = searchTab.getLayoutX() + searchTab.getWidth();
        double animationTimeMaxSeconds = 0.50;
        double searchTabVelocity = (openX - closedX) / animationTimeMaxSeconds;
        moveSearchTabAnimation.setNode(searchTab);
        double deltaX = searchTab.getTranslateX();
        double currentX = searchTab.getLayoutX() + deltaX;
        double animationTime;
        double endingX;
        if(searchTabIsOpen) {
            double distance = currentX - closedX;
            animationTime = distance / searchTabVelocity;
            endingX = closedX;

        }
        else {
            double distance = openX - currentX;
            animationTime =  distance / searchTabVelocity;
            endingX = openX;
        }
        searchTabIsOpen = !searchTabIsOpen;
        moveSearchTabAnimation.setToX(endingX);
        moveSearchTabAnimation.setDuration(Duration.seconds(animationTime));
        moveSearchTabAnimation.play();
    }

}
