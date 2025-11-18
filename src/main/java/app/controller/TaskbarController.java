package app.controller;

import app.entity.LoggedInAccount;
import app.entity.LoggedInProfile;
import app.entity.Profile;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskbarController implements RootController.PageAware, RootController.EditArtistProfileAware {

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());

    @FXML
    private Button settingsButton;
    @FXML
    private Circle settingsProfileImage;
    @FXML
    private Button loginButton;
    @FXML
    private Tooltip loginTooltip;

    // Handles navigation and profile routing
    private Consumer<String> onPageRequest;
    private Consumer<Profile> onEditArtistProfileRequest;

    @Override
    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @Override
    public void setEditArtistProfileProvider(Consumer<Profile> provider) {
        if (provider == null) {
            LOG.warning("Edit artist profile provider not supplied; profile button disabled.");
            this.onEditArtistProfileRequest = null;
            return;
        }
        this.onEditArtistProfileRequest = provider;
    }

    @FXML
    private void initialize() {
        if (settingsButton != null) {
            settingsButton.managedProperty().bind(settingsButton.visibleProperty());
        }
        LoggedInProfile.profileProperty().addListener((obs, oldVal, newVal) -> refreshSettingsButton());
        LoggedInAccount.accountProperty().addListener((obs, oldVal, newVal) -> refreshLoginTooltip());
        refreshSettingsButton();
        refreshLoginTooltip();
    }

    @FXML
    private void handleClick(ActionEvent e) {
        Object src = e.getSource();
        if (!(src instanceof Button b))
            return;

        if (onPageRequest == null) {
            LOG.warning("Page request handler not set; button ignored");
            return;
        }

        switch (b.getId()) {
            case "workspaceButton" -> onPageRequest.accept("workspace");
            case "exploreButton" -> onPageRequest.accept("explore");
            case "mapButton" -> onPageRequest.accept("map");
            case "loginButton" -> handleLoginButton();
            case "settingsButton" -> handleSettingsClick();

            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }

    }

    private void handleSettingsClick() {
        if (onEditArtistProfileRequest == null) {
            LOG.warning("Edit artist profile handler not set; ignoring settings click");
            return;
        }
        Profile profile = LoggedInProfile.getInstance();
        if (profile == null) {
            LOG.warning("No logged-in profile available; showing alert");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Artist Login Required");
            alert.setHeaderText(null);
            alert.setContentText("Not logged in as artist.");
            alert.showAndWait();
            return;
        }
        onEditArtistProfileRequest.accept(profile);
    }

    private void refreshSettingsButton() {
        if (settingsButton == null)
            return;
        Profile profile = LoggedInProfile.getInstance();
        boolean hasProfile = profile != null;
        settingsButton.setVisible(hasProfile);
        if (settingsProfileImage == null) {
            return;
        }
        if (hasProfile && profile.getProfilePictureURL() != null && !profile.getProfilePictureURL().isBlank()) {
            Image image = new Image(profile.getProfilePictureURL(), 48, 48, true, true);
            settingsProfileImage.setFill(new ImagePattern(image));
        } else {
            settingsProfileImage.setFill(null);
        }
    }

    private void refreshLoginTooltip() {
        if (loginTooltip == null)
            return;
        boolean loggedIn = LoggedInAccount.getInstance() != null;
        loginTooltip.setText(loggedIn ? "Logout" : "Login");
    }

    private void handleLoginButton() {
        boolean loggedIn = LoggedInAccount.getInstance() != null;
        if (loggedIn) {
            LoginController.logout();
            if (loginTooltip != null) {
                loginTooltip.setText("Login");
            }
        }
        onPageRequest.accept("login");
    }
}
