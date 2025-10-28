package app;

import app.boundary.ModelWorkspaceBoundary;
import app.controller.ModelWorkspaceController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Entry point that wires the boundary and controller layers together.
 */
public class ModelWorkspaceApp extends Application {
    private final ModelWorkspaceController controller = new ModelWorkspaceController();
    private final ModelWorkspaceBoundary boundary = new ModelWorkspaceBoundary(controller);

    @Override
    public void start(Stage stage) {
        controller.initialize(stage);

        var root = boundary.build(stage);
        Scene scene = new Scene(root, 1280, 800, true);
        scene.getStylesheets().add(
            Objects.requireNonNull(ModelWorkspaceApp.class.getResource("/app/styles.css")).toExternalForm()
        );

        stage.setTitle("3D Model Workspace");
        stage.setScene(scene);
        stage.show();

        controller.loadInitialModel();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
