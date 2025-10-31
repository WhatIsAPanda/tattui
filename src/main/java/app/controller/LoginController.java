package app.controller;
import java.sql.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    public void registerClicked(ActionEvent e) {
        System.out.println("registerClicked");
    }
    public void signInClicked(ActionEvent e) throws SQLException {
        String url = "jdbc:mysql://hopper.proxy.rlwy.net:28919/railway";
        String user = "root";
        String password = "lLcIaxfWngHgBcPljtAUtuoebZSDSPtz";
        Connection con = DriverManager.getConnection(url,user, password);
        PreparedStatement stmt = con.prepareStatement("SELECT password FROM Users WHERE username = ?");
        stmt.setString(1,usernameField.getText());
        ResultSet rs = stmt.executeQuery();
        if(rs.isBeforeFirst()) {
            System.out.println("Login failed");
        }
        else if(rs.getString("password").equals(passwordField.getText())) {
            System.out.println("Login successful");
        }
        else {
            System.out.println("Login failed");
        }


        con.close();
    }
}
