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

    /** Preferred: supports both classpath resources and http/https URLs. */
    public void setData(String name, String bio, String imagePath) {
        artistName.setText(name == null ? "" : name);
        artistBio.setText(bio == null ? "" : bio);
        artistBio.setWrapText(true);

        if (imagePath == null || imagePath.isBlank()) {
            // leave empty; caller may provide fallback in FXML style
            return;
        }

        try {
            Image img;
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                // Load directly from the web (Cloudinary). Background loading = true.
                img = new Image(imagePath, 320, 320, true, true, true);
            } else {
                // Try as classpath resource
                var in = getClass().getResourceAsStream(imagePath);
                if (in != null) {
                    img = new Image(in, 320, 320, true, true);
                } else {
                    // As a last resort, try treating it as a file/URL string anyway
                    img = new Image(imagePath, 320, 320, true, true, true);
                }
            }
            artistImage.setImage(img);
        } catch (Exception ignored) {
            // no crash; just keep whatever was there
        }

        // Light UX polish without touching FXML:
        if (artistImage != null) {
            artistImage.setPreserveRatio(true);
            artistImage.setFitWidth(240);
            artistImage.setFitHeight(240);
            artistImage.setSmooth(true);
        }
        if (artistName != null) {
            artistName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        }
        if (artistBio != null) {
            artistBio.setStyle("-fx-text-fill: #ddd;");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) artistName.getScene().getWindow();
        stage.close();
    }
}
