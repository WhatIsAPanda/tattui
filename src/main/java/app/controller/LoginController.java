package app.controller;
import java.io.IOException;
import java.sql.*;

import app.Main;
import app.entity.LoggedInUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    public void registerClicked(ActionEvent e) {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/app/LoginpagePretty.fxml"));

    }
    public void signInClicked(ActionEvent e) throws SQLException, IOException {
        String url = "jdbc:mysql://hopper.proxy.rlwy.net:28919/railway";
        String user = "root";
        String password = "lLcIaxfWngHgBcPljtAUtuoebZSDSPtz";
        Connection con = DriverManager.getConnection(url,user, password);
        PreparedStatement stmt = con.prepareStatement("SELECT password FROM Users WHERE username = ?");
        String typedUsername = usernameField.getText();
        stmt.setString(1,typedUsername);
        ResultSet rs = stmt.executeQuery();
        if(rs.next() && rs.getString("password").equals(passwordField.getText())) {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/app/Root.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root,1280,800);
            Stage stage = currentStage();
            stage.setScene(scene);
            LoggedInUser.setUsername(typedUsername);
        }
        else {
            System.out.println("Login failed");
        }

        con.close();
    }
    private Stage currentStage() {
        if (usernameField.getScene() == null) return null;
        return usernameField.getScene().getWindow() instanceof Stage s ? s : null;
    }
}
