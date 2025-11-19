package app;

import app.util.ImageResolver;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/app/view/Root.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
            Objects.requireNonNull(Main.class.getResource("/app/css/main.css")).toExternalForm()
        );
        scene.getRoot().lookupAll("*").forEach(n -> n.setFocusTraversable(false));

        stage.getIcons().add(ImageResolver.load("/icons/code.png"));
        stage.setTitle("Tattui");
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
