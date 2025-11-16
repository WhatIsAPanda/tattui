package app.controller;

import app.boundary.ViewProfileBoundary;
import app.entity.Profile;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RootController {

    @FXML private BorderPane rootPane;
    @FXML private HBox taskbarContainer;
    @FXML private AnchorPane workspaceContainer;

    private WorkspaceController workspaceController;
    private final Map<String, Parent> pageCache = new HashMap<>();
    private static final String WORKSPACE_PAGE = "workspace";

    private static final Map<String, String> PAGE_PATHS = Map.of(
        WORKSPACE_PAGE, "/app/view/Workspace.fxml",
        "map", "/app/view/Map.fxml",
        "login", "/app/view/Login.fxml",
        "EditMyProfile", "/app/view/EditMyProfile.fxml",
        "viewProfile", "/app/view/viewProfile.fxml",
        "explore", "/app/view/Explore.fxml"
    );

    public RootController() {
        // Default constructor required for JavaFX FXML loader.
    }

    public interface PageAware {
        void setOnPageRequest(Consumer<String> pageRequestHandler);
    }

    public interface WorkspaceAware {
        void setWorkspaceProvider(Supplier<WorkspaceController> provider);
    }
    public interface ProfileAware {
        void setProfileProvider(Consumer<Profile> provider);
    }


    // --- Initialization ---

    @FXML
    public void initialize() {
        loadView("/app/view/Taskbar.fxml");
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

    // no global singleton accessor anymore

    // --- Navigation ---

    /** Displays a page by key. Can be called from any controller. */
    public void showPage(String key) {
        showPage(key, Optional.empty());
    }

    /** Displays a page by key. Can be called from any controller. */
    public void showPage(String key, Optional<Profile> profile) {
        String path = PAGE_PATHS.get(key);
        if (path == null)
            throw new IllegalArgumentException("Unknown page key: " + key);

        Parent view = pageCache.computeIfAbsent(key, k ->{
            if (profile.isPresent()) {
                return loadView(path, profile);
            }
                else {
                    return loadView(path, Optional.empty());
                }
            });

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
        return loadView(path, Optional.empty());
    }

    private Parent loadView(String path, Optional<Profile> profile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();
            

            Object cntrl = loader.getController();
            if (cntrl instanceof PageAware aware)
                aware.setOnPageRequest(this::showPage);
            if (cntrl instanceof ProfileAware aware) {
                aware.setProfileProvider((a) -> this.showPage("EditMyProfile",Optional.of(a))); //not elegant but works need to add view profile later
            }
            if(cntrl instanceof ViewProfileBoundary viewBoundary) {
                profile.ifPresent(viewBoundary::setProfile);
            }

            if ("/app/view/Workspace.fxml".equals(path))
                workspaceController = loader.getController();
            if ("/app/view/Taskbar.fxml".equals(path))
                attachContent(taskbarContainer, view);
            if (cntrl instanceof WorkspaceAware aware)
                aware.setWorkspaceProvider(() -> workspaceController);
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
}
