package app.controller;

import java.util.function.Consumer;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;

import app.controller.RootController.PageAware;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import com.gluonhq.maps.MapLayer;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Pair;

/**
 * Placeholder controller for the Map view. Provides no-op handlers so the view can be displayed.
 */
public class MapController implements PageAware {
    
    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> resultsList;

    @FXML
    private StackPane mapContainer; // replace WebView in FXML with StackPane id="mapContainer"

    @FXML
    public void initialize() {
        MapView map = new MapView();
        MapPoint center = new MapPoint(41.892718, 12.476259);
        map.setCenter(center);
        map.setZoom(10);
        map.addLayer(new DotLayer(center));
        mapContainer.getChildren().add(map);
    }

    // Inner layer class
    static class DotLayer extends MapLayer {
        private final MapPoint p;
        DotLayer(MapPoint p) { this.p = p; }
        @Override
        protected void layoutLayer() {
            getChildren().clear();
            Node dot = new Circle(5, Color.RED);
            Point2D mp = getMapPoint(p.getLatitude(), p.getLongitude());
            dot.setTranslateX(mp.getX());
            dot.setTranslateY(mp.getY());
            getChildren().add(dot);
        }
    }


    @FXML
    private void handleSearch(ActionEvent event) {
        // TODO: implement search logic
    }

    private Consumer<String> onPageRequest;

    public void setOnPageRequest(Consumer<String> handler) {
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
        // TODO
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
