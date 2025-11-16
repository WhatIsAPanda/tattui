package app.boundary;

import app.entity.DatabaseConnector;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.*;

public class RegisterBoundary {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private ComboBox<String> userTypeComboBox;

    @FXML
    public void initialize() {
        userTypeComboBox.getItems().clear();
        userTypeComboBox.getItems().add("Default");
        userTypeComboBox.getItems().add("Artist");
    }


    public void createAccountClicked() throws SQLException {
        if(usernameField.getText().isEmpty() || passwordField.getText().isEmpty() || confirmPasswordField.getText().isEmpty()) {
            errorLabel.setText("Please fill all the fields");
            return;
        }
        if(!passwordField.getText().equals(confirmPasswordField.getText())) {
            errorLabel.setText("Passwords do not match");
            return;
        }
        if(userTypeComboBox.getValue() == null) {
            errorLabel.setText("Please select a user type");
        }
        try {
            boolean isArtist = userTypeComboBox.getValue().equals("Artist");
            String username = usernameField.getText();
            String password = passwordField.getText();
            DatabaseConnector.createUser(username, password, isArtist);
            errorLabel.setText("Account created successfully");
        }
        catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }
}

