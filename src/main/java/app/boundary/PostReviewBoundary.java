package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.LoggedInAccount;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.sql.SQLException;

public final class PostReviewBoundary {

    private static final String DEFAULT_AVATAR = "/icons/artist_raven.jpg";

    @FXML
    private Label artistNameLabel;
    @FXML
    private Label artistBioLabel;
    @FXML
    private Circle artistAvatar;
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
    private String selectedImageUrl;
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
        if (profile == null) {
            return;
        }
        if (artistNameLabel != null) {
            artistNameLabel.setText("@" + profile.getUsername());
        }
        if (artistBioLabel != null) {
            String bio = (profile.biography == null || profile.biography.isBlank())
                    ? "No biography yet."
                    : profile.biography;
            artistBioLabel.setText(bio);
        }
        if (artistAvatar != null) {
            String avatarUrl = profile.getProfilePictureURL();
            Image avatarImg = (avatarUrl == null || avatarUrl.isBlank())
                    ? new Image(DEFAULT_AVATAR)
                    : new Image(avatarUrl, 80, 80, true, true);
            artistAvatar.setFill(new ImagePattern(avatarImg));
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
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Review Photo");
        dialog.setHeaderText("Enter the URL to the image you want to attach");
        dialog.setContentText("Image URL:");
        if (dialogStage != null) {
            dialog.initOwner(dialogStage);
        }
        dialog.showAndWait().ifPresent(url -> {
            String trimmed = url == null ? null : url.trim();
            if (trimmed == null || trimmed.isEmpty()) {
                selectedImageUrl = null;
                if (uploadStatusLabel != null) {
                    uploadStatusLabel.setText("No photo selected");
                }
            } else {
                selectedImageUrl = trimmed;
                if (uploadStatusLabel != null) {
                    uploadStatusLabel.setText(trimmed);
                }
            }
        });
    }

    @FXML
    private void handlePostReview() {
        if (profile == null) {
            alert(Alert.AlertType.ERROR, "No artist selected",
                    "Unable to post review because no artist profile was provided.");
            return;
        }
        if (LoggedInAccount.getInstance() == null) {
            alert(Alert.AlertType.ERROR, "Login required", "You must be logged in to post a review.");
            return;
        }
        String text = reviewTextArea != null ? reviewTextArea.getText() : "";
        if (text == null || text.trim().length() < 1 || text.trim().length() > 200) {
            alert(Alert.AlertType.WARNING, "Invalid length", "Reviews must be between 1 and 200 characters.");
            return;
        }
        int rating = ratingSlider != null ? (int) Math.round(ratingSlider.getValue()) : 0;
        rating = Math.max(0, Math.min(5, rating));

        String photoPath = (selectedImageUrl != null && !selectedImageUrl.isBlank()) ? selectedImageUrl : null;

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
