package app.controller;

import app.entity.LoggedInUser;
import app.entity.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskbarController implements RootController.PageAware, RootController.ProfileAware {

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());

    // Handles navigation and profile routing
    private Consumer<String> onPageRequest;
    private Consumer<Profile> onProfileRequest;

    @Override
    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @Override
    public void setProfileProvider(Consumer<Profile> provider) {
        this.onProfileRequest = provider;
    }

    @FXML
    private void handleClick(ActionEvent e) {
        Object src = e.getSource();
        if (!(src instanceof Button b)) return;

        if (onPageRequest == null) {
            LOG.warning("Page request handler not set; button ignored");
            return;
        }

        switch (b.getId()) {
            case "workspaceButton" -> onPageRequest.accept("workspace");
            case "exploreButton" -> onPageRequest.accept("explore");
            case "mapButton" -> onPageRequest.accept("map");
            case "loginButton" -> onPageRequest.accept("login");
            case "logoButton" -> onProfileRequest.accept(LoggedInUser.getInstance());
            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }
        
    }
}
