package app.controller;

import app.boundary.ViewMyProfileBoundary;
import app.entity.DatabaseConnector;
import app.entity.LoggedInProfile;
import app.entity.Profile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RootController {

    @FXML private BorderPane rootPane;
    @FXML private HBox taskbarContainer;
    @FXML private AnchorPane workspaceContainer;

    private WorkspaceController workspaceController;
    private final Map<String, Parent> pageCache = new HashMap<>();
    private static final String WORKSPACE_PAGE = "workspace";
    private static final String LOGIN_PAGE = "login";
    private static RootController ctrl = null;

    private static final Map<String, String> PAGE_PATHS = Map.of(
        WORKSPACE_PAGE, "/app/view/Workspace.fxml",
        "map", "/app/view/Map.fxml",
        "gallery", "/app/view/Gallery.fxml",
        LOGIN_PAGE, "/app/view/Login.fxml",
        "viewProfile", "/app/view/viewMyProfile.fxml",
            "register","/app/view/Register.fxml"
    );
    private static final Set<String> DATABASE_PAGES = Set.of("map", "explore");

    // --- Initialization ---

    @FXML
    public void initialize() {
        loadView("/app/view/Taskbar.fxml");
        showPage(WORKSPACE_PAGE);
        DatabaseConnector.ensureConnection();
        if(getInstance() == null) {
            setInstance();
        }
        rootPane.sceneProperty().addListener((obs, o, n) -> {
            if (n != null && n.getWindow() instanceof Stage stage)
                notifyWorkspaceStage(stage);
        });

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage != null) notifyWorkspaceStage(stage);
        });
    }

    private void setInstance() {
        ctrl = this;
    }
    // --- Singleton Accessor ---
    public static RootController getInstance() {
        return ctrl;
    }

    // --- Navigation ---

    /** Displays a page by key. Can be called from any controller. */
    public void showPage(String key) {
        showPage(key, null);
    }

    /** Displays a page by key. Can be called from any controller. */
    public void showPage(String key, Profile profile) {
        String path = PAGE_PATHS.get(key);
        if (path == null)
            throw new IllegalArgumentException("Unknown page key: " + key);
        if (DATABASE_PAGES.contains(key) && !DatabaseConnector.ensureConnection()) {
            showDatabaseAlert();
            return;
        }
        if(key.equals(LOGIN_PAGE)) {
            pageCache.clear();
        }

        Parent view = pageCache.computeIfAbsent(key, k ->{
            if (profile != null) {
                return loadView(path, profile);
            }
                else {
                    return loadView(path, null);
                }
            });

        //Hide taskbar for pages on login
        boolean showTaskbar = !key.equals(LOGIN_PAGE) && !key.equals("register");
        taskbarContainer.setVisible(showTaskbar);
        taskbarContainer.setManaged(showTaskbar);

        attachContent(workspaceContainer, view);
        //weird workspace specific issue to be fixed
        if (WORKSPACE_PAGE.equals(key) && workspaceController != null)
            Optional.ofNullable(currentStage())
                    .ifPresent(workspaceController::attachStage);
    }

    private Parent loadView(String path) {
        return loadView(path, null);
    }

    private Parent loadView(String path, Profile profile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            Object cntrl = loader.getController();
            if ("/app/view/Workspace.fxml".equals(path))
                workspaceController = loader.getController();
            if ("/app/view/Taskbar.fxml".equals(path)) {
                attachContent(taskbarContainer, view);
                if(loader.getController() instanceof TaskbarController taskbarController) {
                    TaskbarController.setInstance(taskbarController);
                }
            }

            if(cntrl instanceof ViewMyProfileBoundary viewProfileBoundaryController && profile != null) {
                viewProfileBoundaryController.setProfile(profile);
            }
            return view;
        } catch (IOException e) {
            throw new ViewLoadException("Failed to load " + path, e);
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

    private void showDatabaseAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Connection");
        alert.setHeaderText("Unable to reach the database");
        alert.setContentText("Please check your network connection and try again.");
        Stage stage = currentStage();
        if (stage != null) {
            alert.initOwner(stage);
        }
        alert.showAndWait();
    }
}
