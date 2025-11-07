package app.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RootController {

    @FXML private BorderPane rootPane;
    @FXML private HBox taskbarContainer;
    @FXML private AnchorPane workspaceContainer;

    private WorkspaceController workspaceController;
    private final Map<String, Parent> pageCache = new HashMap<>();

    private static RootController instance;
    private static final String WORKSPACE_PAGE = "workspace";

    private static final Map<String, String> PAGE_PATHS = Map.of(
        WORKSPACE_PAGE, "/app/Workspace.fxml",
        "map", "/app/Map.fxml",
        "gallery", "/app/Gallery.fxml",
        "login", "/app/Login.fxml"
    );

    public RootController() {
        instance = this;
    }

    // --- Initialization ---

    @FXML
    public void initialize() {
        loadTaskbar();
        showPage(WORKSPACE_PAGE);

        rootPane.sceneProperty().addListener((obs, o, n) -> {
            if (n != null && n.getWindow() instanceof Stage stage)
                notifyWorkspaceStage(stage);
        });

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage != null) notifyWorkspaceStage(stage);
        });
    }

    // --- Singleton Accessor ---

    /** Provides global access to the active RootController instance. */
    public static RootController getInstance() {
        return instance;
    }

    // --- Navigation ---

    /** Displays a page by key. Can be called from any controller. */
    public void showPage(String key) {
        String path = PAGE_PATHS.get(key);
        if (path == null)
            throw new IllegalArgumentException("Unknown page key: " + key);

        Parent view = pageCache.computeIfAbsent(key, k -> loadView(path));

        //Hide taskbar for pages on login
        boolean showTaskbar = !"login".equals(key);
        taskbarContainer.setVisible(showTaskbar);
        taskbarContainer.setManaged(showTaskbar);

        attachContent(workspaceContainer, view);
        //weird workspace specific issue to be fixed
        if (WORKSPACE_PAGE.equals(key) && workspaceController != null)
            Optional.ofNullable(currentStage())
                    .ifPresent(workspaceController::attachStage);
    }

    private Parent loadView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            if ("/app/Workspace.fxml".equals(path))
                workspaceController = loader.getController();

            return view;
        } catch (IOException e) {
            throw new ViewLoadException("Failed to load " + path, e);
        }
    }

    // --- Taskbar ---
    private void loadTaskbar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/taskbar.fxml"));
            Parent view = loader.load();
            TaskbarController controller = loader.getController();
            attachContent(taskbarContainer, view);

            // delegate navigation to showPage
            controller.setOnPageRequest(this::showPage);
        } catch (IOException e) {
            throw new ViewLoadException("Failed to load taskbar", e);
        }
    }

    // --- Stage helpers ---

    private void notifyWorkspaceStage(Stage s) {
        if (workspaceController != null)
            workspaceController.attachStage(s);
    }

    private Stage currentStage() {
        if (rootPane.getScene() == null)
            return null;
        return rootPane.getScene().getWindow() instanceof Stage s ? s : null;
    }

    // --- Layout helpers ---

    private void attachContent(Pane parent, Node child) {
        parent.getChildren().setAll(child);
        if (parent instanceof AnchorPane _) {
            AnchorPane.setTopAnchor(child, 0.0);
            AnchorPane.setRightAnchor(child, 0.0);
            AnchorPane.setBottomAnchor(child, 0.0);
            AnchorPane.setLeftAnchor(child, 0.0);
        }
    }

    public static class ViewLoadException extends RuntimeException {
        public ViewLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
