package app.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.*;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;


    public void createAccountClicked() throws SQLException {
        if(usernameField.getText().isEmpty() || passwordField.getText().isEmpty() || confirmPasswordField.getText().isEmpty()) {
            errorLabel.setText("Please fill all the fields");
            return;
        }
        if(!passwordField.getText().equals(confirmPasswordField.getText())) {
            errorLabel.setText("Passwords do not match");
            return;
        }
        String url = "jdbc:mysql://hopper.proxy.rlwy.net:28919/railway";
        String user = "root";
        String password = "lLcIaxfWngHgBcPljtAUtuoebZSDSPtz";
        Connection con = DriverManager.getConnection(url,user, password);
        PreparedStatement stmt = con.prepareStatement("SELECT * FROM Users WHERE username=?");
        stmt.setString(1,usernameField.getText());
        ResultSet rs = stmt.executeQuery();
        while(rs.next()) {
            System.out.println(rs.getString("username"));
        }
        //TODO: needs to check for duplicates correctly, this is wrong
        if(!rs.isBeforeFirst()) {
            stmt = con.prepareStatement("INSERT INTO Users (username,password) VALUES (?,?)");
            stmt.setString(1,usernameField.getText());
            stmt.setString(2,passwordField.getText());
            if(stmt.executeUpdate() != 0) {
                errorLabel.setText("Account created successfully");
            }
        }
        else {
            errorLabel.setText("Username is already taken");
        }
        con.close();

    }
}

