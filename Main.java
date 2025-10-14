package yourpackage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InstagramSidebarApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("InstagramSidebar.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Instagram Vertical Bar");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
