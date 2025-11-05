package app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

/**
 * Root controller that manages navigation between multiple FXML pages
 * (workspace, map, gallery, etc.) and centralizes loading logic.
 */
public class RootController {

    @FXML private BorderPane rootPane;
    @FXML private HBox taskbarContainer;
    @FXML private AnchorPane workspaceContainer;

    private WorkspaceController workspaceController;
    private TaskbarController taskbarController;

    // Cache of loaded views
    private final Map<String, Parent> pageCache = new HashMap<>();

    // FXML path registry
    private static final Map<String, String> PAGE_PATHS = Map.of(
        "workspace", "/app/Workspace.fxml",
        "map", "/app/Map.fxml",
        "gallery", "/app/Gallery.fxml",
            "profile", "/app/viewMyProfile.fxml",
            "search", "/app/SearchPage.fxml"
    );

    @FXML
    public void initialize() {
        loadTaskbar();
        showPage("workspace"); // Default page

        // Handle stage assignment once available
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof Stage stage) {
                notifyWorkspaceStage(stage);
            }
        });

        Platform.runLater(() -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() instanceof Stage stage) {
                notifyWorkspaceStage(stage);
            }
        });
    }

    private void loadTaskbar() {
        taskbarController = loadController("/app/taskbar.fxml", taskbarContainer, TaskbarController.class);
        if (taskbarController != null) {
            taskbarController.setNavigator(new TaskbarController.PageNavigator() {
                @Override public void showWorkspace() { showPage("workspace"); }
                @Override public void showMap() { showPage("map"); }
                @Override public void showGallery() { showPage("gallery"); }
                @Override public void showProfile() {showPage("profile");}
                @Override public void showSearch() {showPage("search");}
            });
        }
    }

    /**
     * Unified page loader — handles caching, loading, and switching.
     */
    private void showPage(String key) {
        String path = PAGE_PATHS.get(key);
        if (path == null)
            throw new IllegalArgumentException("Unknown page key: " + key);

        Parent view = pageCache.computeIfAbsent(key, k -> loadView(path));

        attachContent(workspaceContainer, view);

        if ("workspace".equals(key) && workspaceController != null) {
            Stage stage = currentStage();
            if (stage != null) workspaceController.attachStage(stage);
        }
    }

    /**
     * Generic loader that attaches a view and returns its controller.
     */
    private <T> T loadController(String fxmlPath, Pane target, Class<T> controllerType) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Parent view = loader.load();
            T controller = loader.getController();
            attachContent(target, view);
            return controllerType.isInstance(controller) ? controller : null;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + fxmlPath, e);
        }
    }

    private Parent loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Parent view = loader.load();
            if ("/app/Workspace.fxml".equals(fxmlPath)) {
                workspaceController = loader.getController();
            }
            return view;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + fxmlPath, e);
        }
    }

    private void notifyWorkspaceStage(Stage stage) {
        if (workspaceController != null) {
            workspaceController.attachStage(stage);
        }
    }

    private Stage currentStage() {
        if (rootPane.getScene() == null) return null;
        return rootPane.getScene().getWindow() instanceof Stage s ? s : null;
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