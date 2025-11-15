package app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class ArtistProfileController {

    @FXML private ImageView artistImage;
    @FXML private Label artistName;
    @FXML private Label artistBio;
    @FXML private Button closeButton;

    public void setData(String name, String bio, String imagePath) {
        artistName.setText(name);
        artistBio.setText(bio);
        if (imagePath != null) {
            artistImage.setImage(new Image(getClass().getResourceAsStream(imagePath)));
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) artistName.getScene().getWindow();
        stage.close();
    }
}
