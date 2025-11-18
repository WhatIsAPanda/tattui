package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.LoggedInAccount;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;

public final class PostReviewBoundary {

    @FXML
    private Label artistNameLabel;
    @FXML
    private Slider ratingSlider;
    @FXML
    private Label ratingValueLabel;
    @FXML
    private TextArea reviewTextArea;
    @FXML
    private Label uploadStatusLabel;

    private Profile profile;
    private Stage dialogStage;
    private File selectedImage;
    private Runnable onReviewPosted;

    @FXML
    private void initialize() {
        if (ratingSlider != null && ratingValueLabel != null) {
            ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                ratingValueLabel.setText(String.format("%.0f / 5", newVal.doubleValue()));
            });
            ratingSlider.setValue(5);
        }
        if (uploadStatusLabel != null) {
            uploadStatusLabel.setText("No photo selected");
        }
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        if (artistNameLabel != null && profile != null) {
            artistNameLabel.setText("@" + profile.getUsername());
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setOnReviewPosted(Runnable callback) {
        this.onReviewPosted = callback;
    }

    @FXML
    private void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select review photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedImage = file;
            if (uploadStatusLabel != null) {
                uploadStatusLabel.setText(file.getName());
            }
        }
    }

    @FXML
    private void handlePostReview() {
        if (profile == null) {
            alert(Alert.AlertType.ERROR, "No artist selected", "Unable to post review because no artist profile was provided.");
            return;
        }
        if (LoggedInAccount.getInstance() == null) {
            alert(Alert.AlertType.ERROR, "Login required", "You must be logged in to post a review.");
            return;
        }
        String text = reviewTextArea != null ? reviewTextArea.getText() : "";
        if (text == null || text.trim().length() < 1 || text.trim().length() > 100) {
            alert(Alert.AlertType.WARNING, "Invalid length", "Reviews must be between 1 and 100 characters.");
            return;
        }
        int rating = ratingSlider != null ? (int) Math.round(ratingSlider.getValue()) : 0;
        rating = Math.max(0, Math.min(5, rating));

        String photoPath = selectedImage != null ? selectedImage.toURI().toString() : null;

        try {
            DatabaseConnector.submitReview(
                    LoggedInAccount.getInstance().getAccount_id(),
                    profile.getAccountId(),
                    photoPath,
                    text.trim(),
                    rating);
            alert(Alert.AlertType.INFORMATION, "Review posted", "Thank you for sharing your experience!");
            if (onReviewPosted != null) {
                onReviewPosted.run();
            }
            if (dialogStage != null) {
                dialogStage.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Failed to post review", ex.getMessage());
        }
    }

    private void alert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(dialogStage);
        alert.showAndWait();
    }
}
