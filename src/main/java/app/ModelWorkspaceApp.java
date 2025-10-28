package app;

import app.controller.WorkspaceController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Convenience launcher that presents only the workspace view.
 */
public class ModelWorkspaceApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ModelWorkspaceApp.class.getResource("/app/Workspace.fxml"));
        Parent root = loader.load();

        WorkspaceController controller = loader.getController();

        Scene scene = new Scene(root, 1280, 800, true);
        scene.getStylesheets().add(
            Objects.requireNonNull(ModelWorkspaceApp.class.getResource("/app/styles.css")).toExternalForm()
        );

        stage.setTitle("3D Model Workspace");
        stage.setScene(scene);

        controller.attachStage(stage);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
