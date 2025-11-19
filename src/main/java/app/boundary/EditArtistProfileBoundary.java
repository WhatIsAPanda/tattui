package app.boundary;

import app.entity.DatabaseConnector;
import app.entity.Post;
import app.entity.Profile;
import app.entity.Review;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EditArtistProfileBoundary extends BaseProfileBoundary {
    private static final String NO_PROFILE_SELECTED_TITLE = "No profile selected";

    @FXML
    private Circle profilePicture;
    @FXML
    private TextArea biographyField;
    @FXML
    private Label artistNameField;
    @FXML
    private GridPane postsPanel;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextField latitudeField;

    private Profile profile;

    @FXML
    public void setProfile(Profile profile) {
        try {
            this.profile = DatabaseConnector.getFullProfile(profile);
            loadProfile();
        } catch (SQLException _) {
            // Leave view empty if the backend profile lookup fails.
        }
    }

    @FXML
    private void handleProfileClick() {
        if (profile == null) {
            showAlert(Alert.AlertType.WARNING, NO_PROFILE_SELECTED_TITLE, "Load a profile before editing.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(profile.getProfilePictureURL());
        dialog.setTitle("Change Profile Picture");
        dialog.setHeaderText("Enter the URL for your new profile picture");
        dialog.setContentText("Image URL:");
        if (profilePicture != null && profilePicture.getScene() != null) {
            dialog.initOwner(profilePicture.getScene().getWindow());
        }

        dialog.showAndWait().ifPresent(input -> {
            String imageUrl = trimToNull(input);
            if (imageUrl == null) {
                showAlert(Alert.AlertType.WARNING, "Invalid URL", "Image URL cannot be empty.");
                return;
            }
            try {
                Image image = new Image(imageUrl, false);
                if (image.isError()) {
                    throw new IllegalArgumentException("Unable to load image");
                }
                profile.setProfilePicture(imageUrl, image);
                DatabaseConnector.modifyUser(profile);
                if (profilePicture != null) {
                    profilePicture.setFill(new ImagePattern(image));
                }
            } catch (IllegalArgumentException _) {
                showAlert(Alert.AlertType.ERROR, "Invalid Image", "Unable to load image from that URL.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Update failed", "Unable to save profile picture: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleSaveChanges() {
        try {
            profile.setBiography(biographyField.getText());
            profile.setWorkLatitude(Double.parseDouble(latitudeField.getText()));
            profile.setWorkLongitude(Double.parseDouble(longitudeField.getText()));
            DatabaseConnector.modifyUser(profile);
            showAlert(Alert.AlertType.INFORMATION, "Profile Saved", "Your profile has been saved.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Save failed", "Unable to save profile: " + e.getMessage());
        } catch (NumberFormatException _) {
            showAlert(Alert.AlertType.ERROR, "Invalid Coordinates",
                    "Please enter valid numbers for latitude and longitude.");
        }

    }

    @FXML
    private void addPost() {
        if (profile == null) {
            showAlert(Alert.AlertType.WARNING, NO_PROFILE_SELECTED_TITLE, "Load a profile before adding posts.");
            return;
        }

        Dialog<PostFormData> dialog = new Dialog<>();
        dialog.setTitle("Add Post");
        dialog.setHeaderText("Share a new completed tattoo");
        if (postsPanel != null && postsPanel.getScene() != null) {
            dialog.initOwner(postsPanel.getScene().getWindow());
        }
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField urlField = new TextField();
        urlField.setPromptText("Image URL");
        TextField captionField = new TextField();
        captionField.setPromptText("Caption (optional)");
        TextField keywordsField = new TextField();
        keywordsField.setPromptText("Keywords (optional)");

        grid.add(new Label("Image URL"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("Caption"), 0, 1);
        grid.add(captionField, 1, 1);
        grid.add(new Label("Keywords"), 0, 2);
        grid.add(keywordsField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            addButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        dialog.setResultConverter(button -> {
            if (button == addButtonType) {
                return new PostFormData(
                        trimToNull(urlField.getText()),
                        trimToNull(captionField.getText()),
                        trimToNull(keywordsField.getText()));
            }
            return null;
        });

        dialog.showAndWait().ifPresent(form -> {
            try {
                Post newPost = DatabaseConnector.addArtistPost(
                        profile.getAccountId(),
                        form.caption(),
                        form.imageUrl(),
                        form.keywords());
                List<Post> updatedPosts = new ArrayList<>(profile.getArtistPosts());
                updatedPosts.add(0, newPost);
                profile.setArtistPosts(updatedPosts);
                populatePosts(postsPanel, updatedPosts);
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Unable to add post", e.getMessage());
            }
        });

    }

    private void loadProfile() {
        populateProfileCommon(profile, profilePicture, biographyField, artistNameField, longitudeField, latitudeField);
        populatePosts(postsPanel, profile.getArtistPosts());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        if (postsPanel != null && postsPanel.getScene() != null
                && postsPanel.getScene().getWindow() instanceof Stage stage) {
            alert.initOwner(stage);
        }
        alert.showAndWait();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record PostFormData(String imageUrl, String caption, String keywords) {
    }
}
