package app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.application.Platform;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class RootController {

    @FXML private BorderPane rootPane;
    @FXML private HBox taskbarContainer;
    @FXML private AnchorPane workspaceContainer;

    private WorkspaceController workspaceController;
    private TaskbarController taskbarController;
    private Parent workspaceView;
    private Parent mapView;

    @FXML
    public void initialize() {
        loadTaskbar();
        loadWorkspace();
        showWorkspacePage();

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage stage) {
                        notifyWorkspaceStage(stage);
                    }
                });
                if (newScene.getWindow() instanceof Stage stage) {
                    notifyWorkspaceStage(stage);
                }
            }
        });

        Platform.runLater(() -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage stage) {
                notifyWorkspaceStage(stage);
            }
        });
    }

    private void loadTaskbar() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/app/taskbar.fxml")));
            Parent taskbar = loader.load();
            taskbarController = loader.getController();
            if (taskbarController != null) {
                taskbarController.setNavigator(new TaskbarController.PageNavigator() {
                    @Override
                    public void showWorkspace() {
                        showWorkspacePage();
                    }

                    @Override
                    public void showMap() {
                        showMapPage();
                    }
                });
            }
            attachContent(taskbarContainer, taskbar);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load taskbar.fxml", ex);
        }
    }

    private void loadWorkspace() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/app/Workspace.fxml")));
            workspaceView = loader.load();
            workspaceController = loader.getController();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Workspace.fxml", ex);
        }
    }

    private void notifyWorkspaceStage(Stage stage) {
        if (workspaceController != null) {
            workspaceController.attachStage(stage);
        }
    }

    private void showWorkspacePage() {
        if (workspaceView == null) {
            loadWorkspace();
        }
        if (workspaceView != null) {
            attachContent(workspaceContainer, workspaceView);
            Stage stage = currentStage();
            if (stage != null && workspaceController != null) {
                workspaceController.attachStage(stage);
            }
        }
    }

    private void showMapPage() {
        if (mapView == null) {
            mapView = loadMapView();
        }
        if (mapView != null) {
            attachContent(workspaceContainer, mapView);
        }
    }

    private Parent loadMapView() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/app/Map.fxml")));
            return loader.load();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load Map.fxml", ex);
        }
    }

    private Stage currentStage() {
        if (rootPane.getScene() == null) {
            return null;
        }
        return rootPane.getScene().getWindow() instanceof Stage stage ? stage : null;
    }

    private void attachContent(Pane parent, Node child) {
        parent.getChildren().setAll(child);
        if (parent instanceof AnchorPane anchor) {
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
        } else if (parent instanceof HBox) {
            HBox.setHgrow(child, Priority.NEVER);
        }
    }
}
