package app.controller;

import app.Main;
import app.entity.LoggedInUser;
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

    public void setData(String name, Image image) {
        usernameText.setText(name);
        ImagePattern pfpImagePattern = new ImagePattern(image);
        profileCircle.setFill(pfpImagePattern);
    }

    @FXML
    private void handleClick(MouseEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/app/viewMyProfile.fxml"));
        LoggedInUser.setUsername(this.usernameText.getText());
        Parent root = loader.load();
        Scene scene = new Scene(root,1280,800);
        Stage stage = currentStage();
        stage.setScene(scene);
    }

    private Stage currentStage() {
        if (profileCircle.getScene() == null) return null;
        return profileCircle.getScene().getWindow() instanceof Stage s ? s : null;
    }
}
