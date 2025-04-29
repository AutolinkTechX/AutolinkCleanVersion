package org.example.pidev.controllers;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class MapDialogController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button currentLocationButton;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;
    @FXML private Label infoLabel;
    @FXML private WebView webView;

    private TextField locationField; // Reference to WorkshopController's locationField
    private Stage stage;
    private String selectedAddress = "";

    public void initializeController(TextField locationField, Stage stage) {
        this.locationField = locationField;
        this.stage = stage;
        initializeWebView();
    }

    private void initializeWebView() {
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Leaflet Map</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                    #map { height: 500px; width: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                    let map, marker;
                    let selectedLocation = { lat: 0, lng: 0, address: '' };
                    const NOMINATIM_BASE_URL = 'https://nominatim.openstreetmap.org/';
                    let lastRequestTime = 0;
                    const REQUEST_INTERVAL = 1000;

                    function initMap() {
                        map = L.map('map').setView([0, 0], 2);
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        }).addTo(map);
                        marker = L.marker([0, 0], { draggable: true }).addTo(map);

                        map.on('click', (e) => {
                            placeMarker(e.latlng);
                        });

                        marker.on('dragend', () => {
                            placeMarker(marker.getLatLng());
                        });
                    }

                    function placeMarker(latlng, address = null) {
                        marker.setLatLng(latlng);
                        selectedLocation.lat = latlng.lat;
                        selectedLocation.lng = latlng.lng;

                        if (address) {
                            selectedLocation.address = address;
                            window.javafx.setInfo(`Selected: ${address}`);
                        } else {
                            reverseGeocode(latlng.lat, latlng.lng);
                        }
                    }

                    function reverseGeocode(lat, lng) {
                        const now = Date.now();
                        if (now - lastRequestTime < REQUEST_INTERVAL) {
                            setTimeout(() => reverseGeocode(lat, lng), REQUEST_INTERVAL - (now - lastRequestTime));
                            return;
                        }
                        lastRequestTime = now;

                        fetch(`${NOMINATIM_BASE_URL}reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`, {
                            headers: { 'User-Agent': 'WorkshopApp/1.0' }
                        })
                            .then(response => response.json())
                            .then(data => {
                                if (data && data.display_name) {
                                    selectedLocation.address = data.display_name;
                                    window.javafx.setInfo(`Selected: ${data.display_name}`);
                                } else {
                                    selectedLocation.address = `${lat},${lng}`;
                                    window.javafx.setInfo(`Selected: ${lat},${lng}`);
                                }
                            })
                            .catch(error => {
                                selectedLocation.address = `${lat},${lng}`;
                                window.javafx.setInfo(`Selected: ${lat},${lng}`);
                            });
                    }

                    function searchLocation(query) {
                        if (!query) return;
                        const now = Date.now();
                        if (now - lastRequestTime < REQUEST_INTERVAL) {
                            setTimeout(() => searchLocation(query), REQUEST_INTERVAL - (now - lastRequestTime));
                            return;
                        }
                        lastRequestTime = now;

                        fetch(`${NOMINATIM_BASE_URL}search?format=json&q=${encodeURIComponent(query)}&addressdetails=1`, {
                            headers: { 'User-Agent': 'WorkshopApp/1.0' }
                        })
                            .then(response => response.json())
                            .then(data => {
                                if (data && data.length > 0) {
                                    const result = data[0];
                                    const latlng = { lat: parseFloat(result.lat), lng: parseFloat(result.lon) };
                                    map.setView(latlng, 15);
                                    placeMarker(latlng, result.display_name);
                                } else {
                                    window.javafx.showAlert('No results found for the search query.');
                                }
                            })
                            .catch(error => {
                                window.javafx.showAlert('Error during search. Please try again.');
                            });
                    }

                    function setCurrentLocation(lat, lng) {
                        const latlng = { lat: parseFloat(lat), lng: parseFloat(lng) };
                        map.setView(latlng, 15);
                        placeMarker(latlng);
                    }

                    function getSelectedLocation() {
                        return selectedLocation;
                    }

                    initMap();
                </script>
            </body>
            </html>
        """;

        webView.getEngine().loadContent(htmlContent);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) webView.getEngine().executeScript("window");
                    window.setMember("javafx", new JavaFXBridge());
                } catch (Exception e) {
                    showAlert("Error", "Failed to set up map communication", e.getMessage());
                }
            } else if (newState == Worker.State.FAILED) {
                showAlert("Error", "Failed to load map", webView.getEngine().getLoadWorker().getException().getMessage());
            }
        });

        webView.getEngine().setOnError(event -> showAlert("WebView Error", "Error in map", event.getMessage()));
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            showAlert("Warning", "Empty Search", "Please enter a search query.");
            return;
        }
        try {
            webView.getEngine().executeScript("searchLocation('" + query.replace("'", "\\'") + "');");
        } catch (Exception e) {
            showAlert("Error", "Search Failed", e.getMessage());
        }
    }

    @FXML
    private void handleCurrentLocation() {
        try {
            webView.getEngine().executeScript("""
                if (navigator.geolocation) {
                    navigator.geolocation.getCurrentPosition(
                        (position) => {
                            window.javafx.setCurrentLocation(position.coords.latitude, position.coords.longitude);
                        },
                        (error) => {
                            window.javafx.showAlert('Geolocation failed: ' + error.message);
                        }
                    );
                } else {
                    window.javafx.showAlert('Geolocation not supported by browser.');
                }
            """);
        } catch (Exception e) {
            showAlert("Error", "Geolocation Failed", e.getMessage());
        }
    }

    @FXML
    private void handleConfirm() {
        try {
            JSObject location = (JSObject) webView.getEngine().executeScript("getSelectedLocation()");
            Double lat = (Double) location.getMember("lat");
            Double lng = (Double) location.getMember("lng");
            String address = (String) location.getMember("address");

            if (lat == 0 && lng == 0) {
                showAlert("Warning", "No Location Selected", "Please select a location on the map.");
                return;
            }

            selectedAddress = address != null && !address.trim().isEmpty() ? address : lat + "," + lng;
            locationField.setText(selectedAddress);
            locationField.setStyle("");
            stage.close();
        } catch (Exception e) {
            showAlert("Error", "Failed to Confirm Location", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }

    private void showAlert(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private class JavaFXBridge {
        public void setInfo(String text) {
            Platform.runLater(() -> {
                infoLabel.setText(text);
                selectedAddress = text.replace("Selected: ", "");
            });
        }

        public void showAlert(String message) {
            Platform.runLater(() -> MapDialogController.this.showAlert("Error", "Map Error", message));
        }

        public void setCurrentLocation(double lat, double lng) {
            Platform.runLater(() -> {
                try {
                    webView.getEngine().executeScript("setCurrentLocation(" + lat + ", " + lng + ");");
                } catch (Exception e) {
                    MapDialogController.this.showAlert("Error", "Geolocation Error", e.getMessage());
                }
            });
        }
    }
}