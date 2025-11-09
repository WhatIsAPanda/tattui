package app.controller;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import app.controller.RootController.PageAware;
import app.entity.Profile;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Placeholder controller for the Map view. Provides no-op handlers so the view can be displayed.
 */
public class MapController implements PageAware {
    
    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> resultsList;

    @FXML
    private WebView mapView;

    @FXML
    public void initialize() {
        WebEngine webEngine = mapView.getEngine();
        String url = getClass().getResource("/app/html/map.html").toExternalForm();
        webEngine.load(url);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // TODO: implement search logic
    }

    private BiConsumer<String, Optional<Profile>> onPageRequest;

    public void setOnPageRequest(BiConsumer<String,Optional<Profile>> handler) {
        this.onPageRequest = handler;
    }
    
    @FXML
    private void handleClear(ActionEvent event) {
        if (searchField != null) {
            searchField.clear();
        }
        if (resultsList != null) {
            resultsList.getItems().clear();
        }
    }

    @FXML
    private void filterBlackwork(ActionEvent event) {

    }

    

    @FXML
    private void filterScript(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterNeoTraditional(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterJapanese(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterMinimalist(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterGeometric(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterWatercolor(ActionEvent event) {
        // TODO
    }

    @FXML
    private void filterTribal(ActionEvent event) {
        // TODO
    }
}
