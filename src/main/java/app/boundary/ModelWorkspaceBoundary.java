package app.boundary;

import app.controller.ModelWorkspaceController;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Boundary responsible for composing the JavaFX scene graph using the controller.
 */
public class ModelWorkspaceBoundary {
    private final ModelWorkspaceController controller;

    public ModelWorkspaceBoundary(ModelWorkspaceController controller) {
        this.controller = controller;
    }

    public BorderPane build(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #eef3f9;");
        ScrollPane controlPanel = controller.createControlPanel(stage);
        StackPane viewerPane = controller.createViewer();

        root.setTop(controller.createToolbar(stage));
        root.setRight(controlPanel);
        root.setCenter(viewerPane);

        controlPanel.setFitToWidth(true);
        controlPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.35));
        controlPanel.maxWidthProperty().bind(root.widthProperty().multiply(0.35));
        viewerPane.prefWidthProperty().bind(root.widthProperty().multiply(0.65));
        return root;
    }
}
