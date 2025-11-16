package app.controller;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;


import app.entity.Profile;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;

import app.controller.MapController.DotLayer;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import app.entity.ProfileCell;
import app.entity.DatabaseConnector;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

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

    private final List<MapLayer> layers = new LinkedList<>();

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
        // map.addLayer(new DotLayer(center));
        mapContainer.getChildren().add(map);

        resultsList.setCellFactory(listView -> new ProfileCell());
    }


    // Inner layer class
    static class DotLayer extends MapLayer {
        private final List<MapPoint> points;
        DotLayer(List<MapPoint> points) { this.points = points; }
        @Override
        protected void layoutLayer() {
            getChildren().clear();
            for (MapPoint p : points) {
                Point2D mp = getMapPoint(p.getLatitude(), p.getLongitude());
                Node dot = new Circle(4, Color.RED);
                dot.setTranslateX(mp.getX());
                dot.setTranslateY(mp.getY());
                getChildren().add(dot);
        }
    }
}
    @FXML
    private void handleSearch(ActionEvent event) {
        resultsList.getItems().clear();
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        if(query.charAt(0) == '#') {
            tagSearch(query);
        }
        boolean foundCity = citySearch(query);
        if (!foundCity) {
            usernameSearch(query);
        }
        populateMapAsync();
        resultsList.getItems().setAll(allResults);
    }

   private void populateMapAsync() {
        if (allResults == null || allResults.isEmpty()) return;

        depopulateMap(); // clear old markers first

        Task<DotLayer> task = new Task<>() {
            @Override
            protected DotLayer call() {
                List<MapPoint> points = new ArrayList<>();
                int count = allResults.size();
                for (int i = 0; i < count; i++) {
                    Profile target = allResults.get(i);
                    points.add(new MapPoint(target.getLatitude(), target.getLongitude()));
                }
                return new DotLayer(points);
            }
        };

        task.setOnSucceeded(e -> {
            depopulateMap();
            DotLayer layer = task.getValue();
            //layer.markDirty();
            map.addLayer(layer);
            layers.add(layer);
            if (!allResults.isEmpty()) {
                double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
                double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

                for (Profile p : allResults) {
                    double lat = p.getLatitude();
                    double lon = p.getLongitude();
                    minLat = Math.min(minLat, lat);
                    maxLat = Math.max(maxLat, lat);
                    minLon = Math.min(minLon, lon);
                    maxLon = Math.max(maxLon, lon);
                }

                // center point
                double centerLat = (minLat + maxLat) / 2.0;
                double centerLon = (minLon + maxLon) / 2.0;

                // approximate zoom level based on spread
                double latDiff = maxLat - minLat;
                double lonDiff = maxLon - minLon;
                double spread = Math.max(latDiff, lonDiff);

                int zoom;
                if (spread < 0.01) zoom = 14;
                else if (spread < 0.05) zoom = 12;
                else if (spread < 0.2) zoom = 10;
                else if (spread < 1.0) zoom = 8;
                else zoom = 6;

                map.setCenter(new MapPoint(centerLat, centerLon));
                map.setZoom(zoom);
            }

            map.requestLayout(); // ensure refresh
            double z = map.getZoom();
            map.setZoom(z + 0.0001);
            map.setZoom(z);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


    private void depopulateMap() {
        for (MapLayer layer : layers) {
            map.removeLayer(layer);
        }
        layers.clear();
    }

    private void tagSearch(String query){
        System.out.println("unhandled");
    }

    private boolean citySearch(String query){
        try {//Geocode city name

            Pair<Double, Double> center = geocodeCity(query.trim());
            if (center == null) {
                return false;
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
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error searching by city: " + e.getMessage());
        }
        return false;
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
                } else {
                    return null;
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
            } else {
                System.out.println("No matching profiles found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error retrieving profiles: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClear(ActionEvent event) {
        depopulateMap();
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
                .filter(item -> item.getStylesList().contains(tag))
                .collect(Collectors.toList())
        );
    }
}
