package app.boundary;

import app.controller.LoginController;
import app.controller.RootController.PageAware;
import app.entity.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoginBoundary implements PageAware{
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    private BiConsumer<String, Optional<Profile>> onPageRequest;

    public void setOnPageRequest(BiConsumer<String, Optional<Profile>> handler) {
        this.onPageRequest = handler;
    }

    @FXML
    public void loginButtonClicked() throws SQLException {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean success = LoginController.verifyUser(username, password);
        if (success) {
            onPageRequest.accept("workspace", Optional.empty());
        } else {
            System.out.println("fail!");
        }
    }
    //TODO:: implement page navigation for login/sign in
    @FXML
    public void registerButtonClicked(ActionEvent event) {
        //showPage
        return;
    }


}
