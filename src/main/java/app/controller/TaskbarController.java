package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.function.Consumer;

public class TaskbarController {

    private Consumer<String> onPageRequest;

    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @FXML
    private void handleClick(ActionEvent e) {
        if (onPageRequest == null) return;
        Object src = e.getSource();
        if (!(src instanceof Button b)) return;

        switch (b.getId()) {
            case "workspaceButton" -> RootController.getInstance().showPage("workspace");
            case "galleryButton" -> RootController.getInstance().showPage("gallery");
            case "mapButton" -> RootController.getInstance().showPage("map");
            case "loginButton" -> RootController.getInstance().showPage("login");
            default -> System.out.println("Unhandled button: " + b.getId());
        }
    }
}
