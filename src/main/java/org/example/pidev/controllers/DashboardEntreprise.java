package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.example.pidev.utils.SessionManager;

import java.io.IOException;

public class DashboardEntreprise {

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Button listBtn;

    @FXML
    private Button partenrsBtn;

    @FXML
    private Button demandeBtn; // Bouton Demandes de recyclage
    @FXML
    private Button statBtn; // Bouton Demandes de recyclage

    @FXML
    private Button refuserBtn;

    @FXML
    private Button accepterbtn;

    @FXML
    private Button calendarBtn;


    @FXML
    private Button accountBtn;

    @FXML
    public void initialize() {
        // Gestionnaire d'événements pour le bouton "Demandes de recyclage"
        demandeBtn.setOnAction(event -> {
            try {
                loadAccordsInMainPane();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Erreur lors du chargement de la page des accords : " + e.getMessage());
            }
        });

        statBtn.setOnAction(event -> {
            try {
                loadStatistiquesInMainPane();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Erreur lors du chargement de la page des statistiques : " + e.getMessage());
            }
        });

       /* calendarBtn.setOnAction(event -> {
            try {
                loadCalendrierInMainPane();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Erreur lors du chargement de la vue calendrier : " + e.getMessage());
            }
        });*/


        // Autres initialisations au besoin...
    }

    // Méthode pour charger les accords dans le panneau principal
    private void loadAccordsInMainPane() throws IOException {
        // Charger le fichier FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShowAccords.fxml"));
        Parent accordsView = loader.load();


        // Passer l'entreprise au contrôleur d'accords
        ShowAccords controller = loader.getController();
        controller.setEntreprise(SessionManager.getCurrentEntreprise());
        // Effacer et définir le contenu dans la zone CENTER du BorderPane
        mainBorderPane.setCenter(accordsView);

        // S'assurer que le contenu s'étend pour remplir l'espace disponible
        if (accordsView instanceof Region) {
            Region region = (Region) accordsView;
            region.setPrefWidth(mainBorderPane.getWidth() - 235); // 235 est la largeur de la barre latérale
            region.setPrefHeight(mainBorderPane.getHeight());

            // Ajouter un écouteur pour redimensionner dynamiquement
            mainBorderPane.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                region.setPrefWidth(newWidth.doubleValue() - 235);
            });

            mainBorderPane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                region.setPrefHeight(newHeight.doubleValue());
            });
        }
    }



    private void loadStatistiquesInMainPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/StatistiquesView.fxml"));
        Parent statView = loader.load();

        // Passer l'entreprise si nécessaire
        StatistiqueMaterielController controller = loader.getController();
        controller.setEntreprise(SessionManager.getCurrentEntreprise()); // à implémenter dans le controller statistiques si besoin

        mainBorderPane.setCenter(statView);

        // Gestion du redimensionnement (comme pour les autres vues)
        if (statView instanceof Region) {
            Region region = (Region) statView;
            region.setPrefWidth(mainBorderPane.getWidth() - 235);
            region.setPrefHeight(mainBorderPane.getHeight());

            mainBorderPane.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                region.setPrefWidth(newWidth.doubleValue() - 235);
            });

            mainBorderPane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                region.setPrefHeight(newHeight.doubleValue());
            });
        }
    }




   /* private void loadStatistiquesInMainPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/StatistiquesView.fxml"));
        Parent statView = loader.load();

        // Passer l’entreprise si nécessaire
        StatistiqueMaterielController controller = loader.getController();
        controller.setEntreprise(SessionManager.getCurrentEntreprise()); // à implémenter dans le controller statistiques si besoin

        mainBorderPane.setCenter(statView);

        // Gestion du redimensionnement (comme pour les autres vues)
        if (statView instanceof Region) {
            Region region = (Region) statView;
            region.setPrefWidth(mainBorderPane.getWidth() - 235);
            region.setPrefHeight(mainBorderPane.getHeight());

            mainBorderPane.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                region.setPrefWidth(newWidth.doubleValue() - 235);
            });

            mainBorderPane.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                region.setPrefHeight(newHeight.doubleValue());
            });
        }
    }


    private void loadCalendrierInMainPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/CalendrierView.fxml"));
        Parent calendarView = loader.load();
        mainBorderPane.setCenter(calendarView);
    }*/



}

