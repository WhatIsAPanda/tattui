package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

/**
 * Handles navigation events originating from the taskbar buttons.
 */
public class TaskbarController {

    public interface PageNavigator {
        void showWorkspace();
        void showMap();
    }

    private PageNavigator navigator;

    public void setNavigator(PageNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void handleWorkspaceClick(ActionEvent event) {
        if (navigator != null) {
            navigator.showWorkspace();
        }
    }

    @FXML
    private void handleMapClick(ActionEvent event) {
        if (navigator != null) {
            navigator.showMap();
        }
    }
}
