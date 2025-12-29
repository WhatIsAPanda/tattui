package app.boundary;

import app.controller.RootController;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class ProfileItemBoundary {
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
}