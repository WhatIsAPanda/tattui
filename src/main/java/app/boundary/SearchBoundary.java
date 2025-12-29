package app.boundary;

import app.controller.SearchController;
import app.entity.Profile;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SearchBoundary {
    @FXML
    private TextField searchField;
    @FXML
    private VBox searchResultsBox;

    @FXML
    public void search_keyPressed(KeyEvent event) throws SQLException, IOException {
        if(event.getCode() == KeyCode.ENTER) {
            if(searchField.getText().isEmpty()) {
                return;
            }
            List<Profile> profileList = SearchController.getRelevantProfiles(searchField.getText());
            searchResultsBox.getChildren().clear();
            for(Profile profile : profileList) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/view/ProfileItem.fxml"));
                HBox profileItem = loader.load();
                ProfileItemBoundary controller = loader.getController();
                controller.setData(profile);
                searchResultsBox.getChildren().add(profileItem);
            }
        }
    }
}
