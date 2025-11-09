package app.controller;

import app.entity.LoggedInUser;
import app.entity.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.controller.RootController.PageAware;

public class TaskbarController implements PageAware{

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());

    //THis shit handles navigation
    private BiConsumer<String, Optional<Profile>> onPageRequest;

    public void setOnPageRequest(BiConsumer<String, Optional<Profile>> handler) {
        this.onPageRequest = handler;
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
            case "workspaceButton" -> onPageRequest.accept("workspace", Optional.empty());
            case "galleryButton" -> onPageRequest.accept("gallery", Optional.empty());
            case "mapButton" -> onPageRequest.accept("map", Optional.empty());
            case "loginButton" -> onPageRequest.accept("login", Optional.empty());
            case "logoButton" -> {
                onPageRequest.accept("myProfile", Optional.of(LoggedInUser.getInstance()));
                System.out.println("button got clicked, accept ran");
            }
            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }
    }
}
