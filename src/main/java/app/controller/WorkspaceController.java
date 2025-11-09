package app.controller;

import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Workspace-facing controller contract that exposes non-UI operations to other application modules.
 */
public interface WorkspaceController {

    /**
     * Provides the JavaFX stage to the workspace implementation so it can wire window-level interactions.
     */
    void attachStage(Stage stage);

    /**
     * Queues a tattoo image so the workspace can place it onto the model once UV placement is available.
     *
     * @param image tattoo texture to stage for placement
     * @param label friendly name to show inside history galleries/logs
     * @return {@code true} if the workspace accepted the image, otherwise {@code false}
     */
    boolean openTattooFromGallery(Image image, String label);
}
