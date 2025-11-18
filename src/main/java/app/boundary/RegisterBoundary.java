package app.boundary;

import app.entity.DatabaseConnector;
import app.controller.LoginController;
import app.controller.RootController;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

import java.sql.*;

public class RegisterBoundary implements RootController.PageAware {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private CheckBox artistProfile;

    private Consumer<String> onPageRequest;

    @FXML
    public void initialize() {
        errorLabel.setText("");
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
        try {
            boolean isArtist = artistProfile.isSelected();
            String username = usernameField.getText();
            String password = passwordField.getText();
            DatabaseConnector.createUser(username, password, isArtist);
            errorLabel.setText("Account created successfully");
            Thread.sleep(1000); // Pause for a moment to show success message
            boolean success = LoginController.verifyUser(username, password);
            if (success) {
                onPageRequest.accept("workspace");
            } else {
                System.out.println("fail!");
            }
        }
        catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin() {
        if (onPageRequest != null) {
            onPageRequest.accept("login");
        }
    }

    @Override
    public void setOnPageRequest(Consumer<String> pageRequestHandler) {
        this.onPageRequest = pageRequestHandler;
    }
}
