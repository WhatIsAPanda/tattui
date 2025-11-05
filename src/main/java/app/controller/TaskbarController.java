package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class TaskbarController {

    public interface PageNavigator {
        void showWorkspace();
        void showMap();
        void showGallery();
        void showProfile();
        void showSearch();
    }

    private PageNavigator navigator;

    public void setNavigator(PageNavigator navigator) {
        this.navigator = navigator;
    }

   @FXML
    private void handleClick(ActionEvent event) {
        if (navigator == null) return;
        Object src = event.getSource();
        if (!(src instanceof Button button)) return;

        switch (button.getId()) {
            case "workspaceButton" -> navigator.showWorkspace();
            case "galleryButton" -> navigator.showGallery();
            case "exploreButton" -> navigator.showSearch();
            case "mapButton" -> navigator.showMap();
            case "loginButton" -> navigator.showProfile();
            case "settingsButton" -> System.out.println("Settings clicked");
            case "logoButton" -> System.out.println("Logo clicked");
            default -> System.out.println("Unhandled button: " + button.getId());
        }
    }
}
