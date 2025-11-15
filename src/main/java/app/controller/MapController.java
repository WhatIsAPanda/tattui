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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import app.entity.Profile;
import app.entity.ProfileCell;
import app.entity.DatabaseConnector;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;


public class MapController implements PageAware {
    
    @FXML
    private TextField searchField;

    @FXML
    private ListView<Profile> resultsList;

    private LinkedList<Profile> allResults;

    @FXML
    private StackPane mapContainer; 

    private MapView map;

    private Consumer<String> onPageRequest;

    public void setOnPageRequest(Consumer<String> handler) {
        this.onPageRequest = handler;
    }

    @FXML
    public void initialize() {
        map = new MapView();
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
            Node dot = new Circle(5, Color.BLACK);
            Point2D mp = getMapPoint(p.getLatitude(), p.getLongitude());
            dot.setTranslateX(mp.getX());
            dot.setTranslateY(mp.getY());
            getChildren().add(dot);
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        if(query.charAt(0) == '#') {
            tagSearch(query);
        
        }
        citySearch(query);
        populateMap();
    }
    private void populateMap() {
        if (map == null || allResults == null) return;
        for (Profile target : allResults) {
            MapPoint p = new MapPoint(target.getLatitude(), target.getLongtitude());
            map.addLayer(new DotLayer(p));
        }   
    }

    private void tagSearch(String query){
        System.out.println("unhandled");
    }

    private void citySearch(String query){
        try {//Geocode city name

            Pair<Double, Double> center = geocodeCity(query.trim());
            if (center == null) {
                System.out.println("City not found: " + query);
                resultsList.getItems().clear();
                return;
            }

            double lat = center.getKey();
            double lon = center.getValue();

            //Define a search radius maybe slider later
            double radius = 0.5;
            double latFrom = lat - radius;
            double latTo = lat + radius;
            double lonFrom = lon - radius;
            double lonTo = lon + radius;

            //Query database for users within bounds
            List<Profile> profiles = DatabaseConnector.getProfilesWithinBounds(latFrom, latTo, lonFrom, lonTo);

            //ui update
            if (profiles != null && !profiles.isEmpty()) {
                //add style fitler logic here
                allResults = new LinkedList<>(profiles);
                resultsList.getItems().setAll(allResults);
            } else {
                resultsList.getItems().clear();
                System.out.println("No profiles found near " + query);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error searching by city: " + e.getMessage());
        }
    }
    
        
    private Pair<Double, Double> geocodeCity(String city) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" + java.net.URLEncoder.encode(city, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "JavaFXApp");
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");
                    return new Pair<>(lat, lon);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void usernameSearch(String query) {
        try {
            // Fetch all profiles whose usernames partially match the query
            List<Profile> profiles = DatabaseConnector.getProfilesLike(query.trim());

            if (profiles != null && !profiles.isEmpty()) {
                allResults = new LinkedList<>(profiles);
                resultsList.getItems().setAll(allResults);
            } else {
                resultsList.getItems().clear();
                System.out.println("No matching profiles found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error retrieving profiles: " + e.getMessage());
        }
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
        tagSearch(filterTag);
    }

    private void applyFilter(String tag) {
        resultsList.getItems().setAll(
            allResults.stream()
                .filter(item -> item.getTags().contains(tag))
                .collect(Collectors.toList())
        );
    }
}
