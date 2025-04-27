package org.example.pidev.controllers;

import com.google.gson.Gson;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.entities.Accord;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.ServiceAccord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendrierE {

    @FXML
    private WebView webView;

    private ServiceAccord accordService = new ServiceAccord();

    private Entreprise entreprise;

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public void initialize() {
        WebEngine webEngine = webView.getEngine();
        webEngine.load(getClass().getResource("/calendrier.html").toExternalForm());

        // Attendre que la page soit chargée
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    // Charger les accords de ta BDD
                    List<Accord> accords = new ServiceAccord().afficher();

                    // Convertir la liste en JSON
                    Gson gson = new Gson();
                    List<Map<String, Object>> eventList = new ArrayList<>();

                    for (Accord accord : accords) {
                        Map<String, Object> event = new HashMap<>();
                        // Titre avec uniquement le nom du matériel
                        event.put("title", accord.getMaterielRecyclable().getName());
                        
                        // Formatter la date avec l'heure
                        LocalDateTime dateCreation = accord.getDateCreation();
                        String startDate = dateCreation.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        event.put("start", startDate);

                        // Utiliser la date de réception si disponible, sinon utiliser la date de création
                        LocalDateTime dateReception = accord.getDateReception() != null 
                            ? accord.getDateReception()
                            : dateCreation;
                        String endDate = dateReception.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        event.put("end", endDate);
                        
                        // Définir la couleur selon le statut du MaterielRecyclable
                        StatutEnum statut = accord.getMaterielRecyclable().getStatut();
                        String color;
                        switch (statut) {
                            case valide:
                                color = "#28a745"; // Vert
                                break;
                            case refuse:
                                color = "#dc3545"; // Rouge
                                break;
                            case en_attente:
                                color = "#ffc107"; // Jaune
                                break;
                            default:
                                color = "#6c757d"; // Gris par défaut
                        }
                        event.put("backgroundColor", color);
                        event.put("borderColor", color);
                        
                        // Ajouter le statut dans les propriétés étendues
                        event.put("extendedProps", new HashMap<String, Object>() {{
                            put("status", statut.toString());
                            put("materiel", accord.getMaterielRecyclable().getName());
                        }});
                        
                        eventList.add(event);
                    }

                    String jsonEvents = gson.toJson(eventList);
                    String script = "window.loadAccords('" + jsonEvents.replace("'", "\\'") + "')";
                    webEngine.executeScript(script);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
