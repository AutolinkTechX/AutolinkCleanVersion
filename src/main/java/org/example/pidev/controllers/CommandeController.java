package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import org.example.pidev.entities.Commande;
import org.example.pidev.entities.User;
import org.example.pidev.services.CommandeService;
import org.example.pidev.utils.MyDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CommandeController {

    @FXML
    private Button confirmButton;

    private CommandeService commandeService;
    private User currentUser;
    private Double totalAmount;
    private String paymentMethod;

    @FXML
    public void initialize() {
        try {
            // Connexion à la base de données et initialisation du service
            Connection connection = MyDatabase.getInstance().getConnection();
            this.commandeService = new CommandeService(connection);
        } catch (Exception e) { // Utilisation de Exception au lieu de SQLException
            // Gestion de l'erreur de connexion à la base de données
            showErrorAlert("Erreur de connexion",
                    "Échec de la connexion à la base de données",
                    e.getMessage());
        }
    }

    @FXML
    public void handleConfirmCommande() {
        if (currentUser == null) {
            showErrorAlert("Erreur",
                    "Utilisateur non connecté",
                    "Veuillez vous connecter avant de passer commande");
            return;
        }

        Commande commande = createCommande();

        // Gérer l'exception SQLException lors de la création de la commande
        try {
            Commande createdCommande = commandeService.createCommande(commande);

            if (createdCommande != null && createdCommande.getId() > 0) {
                showSuccessAlert(createdCommande.getId());
            } else {
                showErrorAlert("Erreur",
                        "Échec de la commande",
                        "La commande n'a pas pu être enregistrée");
            }
        } catch (SQLException e) {
            // Affichage d'une alerte d'erreur si l'exception SQLException est lancée
            showErrorAlert("Erreur de base de données",
                    "Échec de la création de la commande",
                    e.getMessage());
        }
    }

    private Commande createCommande() {
        Commande commande = new Commande();
        commande.setClient(currentUser);
        commande.setDateCommande(LocalDateTime.now());
        commande.setModePaiement(paymentMethod != null ? paymentMethod : "en ligne");
        commande.setTotal(totalAmount);
        return commande;
    }



    private void showSuccessAlert(int commandId) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Confirmation de commande");
        alert.setHeaderText("Commande validée !");
        alert.setContentText(String.format(
                "Votre commande n°%d a été enregistrée.\n" +
                        "Montant total : %.2f €\n" +
                        "Mode de paiement : %s",
                commandId, totalAmount, paymentMethod));
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setTotalAmount(Double amount) {
        this.totalAmount = amount;
    }

    public void setPaymentMethod(String method) {
        this.paymentMethod = method;
    }
}
