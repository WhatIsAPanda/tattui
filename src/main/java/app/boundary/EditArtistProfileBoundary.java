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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    @FXML
    private ComboBox<String> styleComboBox;
    @FXML
    private Label styles;
    @FXML
    private Button deleteTagButton;
    @FXML
    private Button addTagButton;

    private Profile profile;
    private List<String> currentStyles;

    @FXML
    public void initialize() {
        styleComboBox.getItems().addAll(
                "#Blackwork", "#Script", "#NeoTraditional",
                "#Japanese", "#Minimalist", "#Geometric",
                "#Watercolor", "#Tribal");
    }

    @FXML
    private void addTag() {
        String selectedStyle = styleComboBox.getValue();
        if (selectedStyle == null || profile == null) {
            return;
        }
        String style = selectedStyle.substring(1); // Remove the '#' character
        if (!currentStyles.contains(style)) {
            currentStyles.add(style);
            styles.setText(String.join(" ", currentStyles.stream().map(s -> "#" + s).toList()));
        }
    }

    @FXML
    private void deleteTag() {
        String selectedStyle = styleComboBox.getValue();
        if (selectedStyle == null || profile == null) {
            return;
        }
        String style = selectedStyle.substring(1); // Remove the '#' character
        if (currentStyles.contains(style)) {
            currentStyles.remove(style);
            styles.setText(String.join(" ", currentStyles.stream().map(s -> "#" + s).toList()));
        }
    }

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
            } catch (SQLException _) {
                showAlert(Alert.AlertType.ERROR, "Update failed", "Unable to save profile picture");
            }
        });
    }

    @FXML
    private void handleSaveChanges() {
        try {
            profile.setBiography(biographyField.getText());
            profile.setWorkLatitude(Double.parseDouble(latitudeField.getText()));
            profile.setWorkLongitude(Double.parseDouble(longitudeField.getText()));
            profile.setStylesList(currentStyles);
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

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Share Your Work");
        dialog.setHeaderText("Submit a completed piece or upload a flash design");
        if (postsPanel != null && postsPanel.getScene() != null) {
            dialog.initOwner(postsPanel.getScene().getWindow());
        }
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab postsTab = new Tab("Posts");
        postsTab.setClosable(false);
        postsTab.setContent(buildPostSubmissionForm());

        Tab designsTab = new Tab("Designs");
        designsTab.setClosable(false);
        designsTab.setContent(buildDesignSubmissionForm());

        tabPane.getTabs().addAll(postsTab, designsTab);
        dialog.getDialogPane().setContent(tabPane);
        dialog.showAndWait();
    }

    private Node buildPostSubmissionForm() {
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

        Button submitPostButton = new Button("Submit Post");
        submitPostButton.setDisable(true);
        urlField.textProperty().addListener((obs, oldVal, newVal) -> submitPostButton
                .setDisable(newVal == null || newVal.trim().isEmpty()));

        Label feedbackLabel = new Label();

        submitPostButton.setOnAction(_ -> {
            PostFormData form = new PostFormData(
                    trimToNull(urlField.getText()),
                    trimToNull(captionField.getText()),
                    trimToNull(keywordsField.getText()));
            if (form.imageUrl() == null) {
                displayFormMessage(feedbackLabel, "Image URL is required.", false);
                return;
            }
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
                urlField.clear();
                captionField.clear();
                keywordsField.clear();
                submitPostButton.setDisable(true);
                displayFormMessage(feedbackLabel, "Post submitted!", true);
            } catch (SQLException _) {
                displayFormMessage(feedbackLabel, "Unable to add post.", false);
            }
        });

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.getChildren().addAll(grid, submitPostButton, feedbackLabel);
        return container;
    }

    private Node buildDesignSubmissionForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField designUrlField = new TextField();
        designUrlField.setPromptText("Design Picture URL");
        TextField designNameField = new TextField();
        designNameField.setPromptText("Design Name");

        grid.add(new Label("Picture URL"), 0, 0);
        grid.add(designUrlField, 1, 0);
        grid.add(new Label("Design Name"), 0, 1);
        grid.add(designNameField, 1, 1);

        Button submitDesignButton = new Button("Submit Design");
        Label feedbackLabel = new Label();
        Runnable toggleDesignButton = () -> submitDesignButton
                .setDisable(
                        trimToNull(designUrlField.getText()) == null || trimToNull(designNameField.getText()) == null);
        designUrlField.textProperty().addListener((obs, oldVal, newVal) -> toggleDesignButton.run());
        designNameField.textProperty().addListener((obs, oldVal, newVal) -> toggleDesignButton.run());
        toggleDesignButton.run();

        submitDesignButton.setOnAction(_ -> {
            String imageUrl = trimToNull(designUrlField.getText());
            String designName = trimToNull(designNameField.getText());
            if (imageUrl == null || designName == null) {
                displayFormMessage(feedbackLabel, "Both fields are required.", false);
                return;
            }
            try {
                DatabaseConnector.addArtistDesign(
                        profile.getAccountId(),
                        designName,
                        imageUrl);
                designUrlField.clear();
                designNameField.clear();
                toggleDesignButton.run();
                displayFormMessage(feedbackLabel, "Design submitted!", true);
            } catch (SQLException _) {
                displayFormMessage(feedbackLabel, "Unable to add design.", false);
            }
        });

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.getChildren().addAll(grid, submitDesignButton, feedbackLabel);
        return container;
    }

    private void loadProfile() {
        populateProfileCommon(profile, profilePicture, biographyField, artistNameField, longitudeField, latitudeField,
                styles);
        currentStyles = new ArrayList<>(profile.getStylesList());
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

    private void displayFormMessage(Label label, String message, boolean success) {
        if (label == null) {
            return;
        }
        label.setText(message);
        label.setStyle(success ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #c62828;");
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
