package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.pidev.entities.Facture;
import org.example.pidev.entities.User;

public class FactureDetailsController {

    @FXML
    private Label idLabel;
    @FXML
    private Label montantLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label commandeLabel;
    @FXML
    private Label clientLabel;

    // Utilisateur statique (comme dans FactureController)
    private static final User STATIC_USER = new User() {{
        setId(6); // Utilisateur statique avec ID 6
        setName("Tasnim");
        setLastName("Ghodbane");
        setEmail("tasnim.ghodbane@esprit.tn");
        setPassword("tasnim123");
        setCreatedAt(java.time.LocalDateTime.parse("2025-02-28T19:16:39"));
    }};

    private Facture facture;

    public void setFacture(Facture facture) {
        this.facture = facture;
        idLabel.setText("Facture #" + facture.getId());
        montantLabel.setText("Montant: " + facture.getMontant() + " DT");
        dateLabel.setText("Date: " + facture.getDatetime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        commandeLabel.setText("Commande: " + facture.getCommande().getId());
        clientLabel.setText("Client: " + STATIC_USER.getName() + " " + STATIC_USER.getLastName()); // Affiche le client connecté
    }
}