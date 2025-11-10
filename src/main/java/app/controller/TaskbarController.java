package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.controller.RootController.PageAware;

public class TaskbarController implements PageAware{

    private static final Logger LOG = Logger.getLogger(TaskbarController.class.getName());

    //THis shit handles navigation
    private Consumer<String> onPageRequest;

    public void setOnPageRequest(Consumer<String> handler) {
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
            case "workspaceButton" -> onPageRequest.accept("workspace");
            case "galleryButton" -> onPageRequest.accept("gallery");
            case "mapButton" -> onPageRequest.accept("map");
            case "loginButton" -> onPageRequest.accept("login");
            case "logoButton" -> onPageRequest.accept("viewProfile");
            default -> LOG.log(Level.WARNING, "Unhandled button: {0}", b.getId());
        }
    }
}
