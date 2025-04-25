package org.example.pidev.utils;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import javafx.application.Platform;
import org.example.pidev.entities.User;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SmsService {
    private static final Logger LOGGER = Logger.getLogger(SmsService.class.getName());

    // Configuration Twilio - À remplacer par vos informations
    private static final String ACCOUNT_SID = "AC2249a817045c5030d299356f56f60c3b";
    private static final String AUTH_TOKEN = "144b02d59996bf20299828de07cdbec3";
    private static final String TWILIO_PHONE_NUMBER = "+19704044639"; // Numéro Twilio acheté (US/Canada)

    static {
        // Initialisation automatique au chargement de la classe
        try {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            LOGGER.info("Twilio initialisé avec succès");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Échec de l'initialisation de Twilio", e);
            throw new RuntimeException("Échec de l'initialisation du service SMS", e);
        }
    }

    public static void sendPaymentConfirmation(User client, String orderId, double amount) {
        if (client == null || client.getPhone() == 0) {
            LOGGER.warning("Client ou numéro de téléphone invalide");
            showErrorAlert("Erreur", "Numéro de téléphone client invalide");
            return;
        }

        String formattedNumber;
        try {
            formattedNumber = formatPhoneNumber(String.valueOf(client.getPhone()));
            if (formattedNumber.equals(TWILIO_PHONE_NUMBER)) {
                throw new IllegalArgumentException("Le numéro du client ne peut pas être le numéro Twilio");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Format de numéro invalide: " + e.getMessage());
            showErrorAlert("Erreur", "Format de numéro invalide");
            return;
        }

        new Thread(() -> {
            try {
                String messageBody = String.format(
                        "Cher client,\n" +
                                "Votre paiement de %.2f DT a été accepté.\n" +
                                "Référence commande: %s\n" +
                                "Merci pour votre confiance!",
                        amount, orderId
                );

                Message message = Message.creator(
                        new PhoneNumber(formattedNumber),
                        new PhoneNumber(TWILIO_PHONE_NUMBER),
                        messageBody
                ).create();

                LOGGER.info("SMS envoyé avec SID: " + message.getSid());
                showSuccessAlert("Succès", "SMS de confirmation envoyé");

            } catch (TwilioException e) {
                LOGGER.log(Level.SEVERE, "Erreur Twilio: " + e.getMessage());
                showErrorAlert("Erreur SMS", "Échec de l'envoi du SMS");
            }
        }).start();
    }

    private static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Numéro vide");
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");

        // Format tunisien local (8 chiffres)
        if (digitsOnly.length() == 8) {
            return "+216" + digitsOnly;
        }

        // Format tunisien avec indicatif (9 chiffres après 216)
        if (digitsOnly.startsWith("216") && digitsOnly.length() == 11) {
            return "+" + digitsOnly;
        }

        // Format international complet
        if (digitsOnly.length() >= 10 && digitsOnly.startsWith("1")) {
            return "+" + digitsOnly; // Pour les numéros US/Canada
        }

        throw new IllegalArgumentException("Format de numéro non supporté: " + phoneNumber);
    }

    private static void showSuccessAlert(String title, String message) {
        Platform.runLater(() ->
                AlertUtils.showInformationAlert(title, message)
        );
    }

    private static void showErrorAlert(String title, String message) {
        Platform.runLater(() ->
                AlertUtils.showErrorAlert(title, message, "")
        );
    }
}