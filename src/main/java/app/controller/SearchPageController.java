package app.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.*;

public class SearchPageController {
    @FXML
    private TextField searchField;
    @FXML
    private VBox searchResultBox;

    @FXML
    public void keyPressed(KeyEvent event) throws SQLException, IOException {
        if(!(event.getCode() == KeyCode.ENTER)){
            return;
        }
        String url = "jdbc:mysql://hopper.proxy.rlwy.net:28919/railway";
        String user = "root";
        String password = "lLcIaxfWngHgBcPljtAUtuoebZSDSPtz";
        Connection con = DriverManager.getConnection(url,user, password);
        PreparedStatement stmt = con.prepareStatement("SELECT profile_picture, username FROM Users WHERE username LIKE ?");
        stmt.setString(1,"%"+searchField.getText()+"%");
        ResultSet rs = stmt.executeQuery();
        searchResultBox.getChildren().clear();
        while(rs.next()){
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/ProfileItem.fxml"));
            HBox profileItem = loader.load();
            ProfileItemController controller = loader.getController();
            String usernameData = rs.getString("username");
            Image profilePictureData = new Image(rs.getString("profile_picture"));
            controller.setData(usernameData,profilePictureData);
            searchResultBox.getChildren().add(profileItem);
        }
    }

}
