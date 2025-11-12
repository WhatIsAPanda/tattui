package app.controller;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;

import app.controller.RootController.PageAware;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import com.gluonhq.maps.MapLayer;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import java.util.LinkedList;
import app.entity.Profile;
import app.entity.ProfileCell;

public class MapController implements PageAware {
    
    @FXML
    private TextField searchField;

    @FXML
    private ListView<Profile> resultsList;

    private LinkedList<Profile> allResults;

    @FXML
    private StackPane mapContainer; 

    @FXML
    public void initialize() {
        MapView map = new MapView();
        MapPoint center = new MapPoint(41.892718, 12.476259);
        map.setCenter(center);
        map.setZoom(10);
        map.addLayer(new DotLayer(center));
        mapContainer.getChildren().add(map);

        resultsList.setCellFactory(listView -> new ProfileCell());
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
    private void filter(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String filterTag = clickedButton.getText(); // e.g. "#Blackwork"
        applyFilter(filterTag);
    }

    private void applyFilter(String tag) {
        resultsList.getItems().setAll(
            allResults.stream()
                .filter(item -> item.getTags().contains(tag))
                .collect(Collectors.toList())
        );
    }
}
