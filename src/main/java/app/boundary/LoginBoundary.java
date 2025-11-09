package app.boundary;

import app.controller.LoginController;
import app.controller.RootController.PageAware;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;
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

    private Consumer<String> onPageRequest;

    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @FXML
    public void loginButtonClicked() throws SQLException {
        String username = usernameField.getText();
        String password = passwordField.getText();
        boolean success = LoginController.verifyUser(username, password);
        if (success) {
            System.out.println("success!");
            onPageRequest.accept("workspace");
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
