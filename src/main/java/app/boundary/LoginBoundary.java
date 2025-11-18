package app.boundary;

import app.controller.LoginController;
import app.controller.RootController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginBoundary implements RootController.PageAware {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Label errorLabel;

    private Consumer<String> onPageRequest;

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    @FXML
    public void loginButtonClicked() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            if (errorLabel != null) {
                errorLabel.setText("Please enter your username and password");
            }
            return;
        }

        try {
            boolean success = LoginController.verifyUser(username, password);
            if (success) {
                if (onPageRequest != null) {
                    onPageRequest.accept("workspace");
                }
            } else if (errorLabel != null) {
                errorLabel.setText("Incorrect username or password");
            }
        } catch (SQLException _) {
            if (errorLabel != null) {
                errorLabel.setText("Unable to log in right now. Please try again.");
            }
        }
    }

    @FXML
    public void registerButtonClicked(ActionEvent event) {
        onPageRequest.accept("register");
    }

    @FXML
    private void handleBackToWorkspace() {
        if (onPageRequest != null) {
            onPageRequest.accept("workspace");
        }
    }

    @Override
    public void setOnPageRequest(Consumer<String> pageRequestHandler) {
        this.onPageRequest = pageRequestHandler;
    }
}
