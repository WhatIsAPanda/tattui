package app.controller;

import app.Main;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileItemController {
    @FXML
    private Label usernameText;
    @FXML
    private Circle profileCircle;
    private Profile profile;

    public void setData(Profile profile) {
        this.profile = profile;
        usernameText.setText(profile.getUsername());
        ImagePattern pfpImagePattern = new ImagePattern(profile.getProfilePicture());
        profileCircle.setFill(pfpImagePattern);
    }

    @FXML
    private void handleClick(MouseEvent event) throws IOException {
        RootController.getInstance().showPage("viewProfile",profile);
    }

    private Stage currentStage() {
        if (profileCircle.getScene() == null) return null;
        return profileCircle.getScene().getWindow() instanceof Stage s ? s : null;
    }
}