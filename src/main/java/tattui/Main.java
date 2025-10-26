package tattui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Load Root.fxml from classpath
        Parent root = FXMLLoader.load(getClass().getResource("/tattui/fxml/Root.fxml"));

        Scene scene = new Scene(root, 1200, 600); // width, height
        stage.setScene(scene);
        stage.setTitle("Tattui Sidebar");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
