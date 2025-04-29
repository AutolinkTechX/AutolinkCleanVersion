package org.example.pidev.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.pidev.entities.Workshop;

import java.io.File;
import java.text.SimpleDateFormat;

public class WorkshopCardController {

    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label placesLabel;
    @FXML private Label startDateLabel;
    @FXML private ImageView workshopImage;

    private static final String UPLOADS_DIR = "/Uploads";
    private static final String DEFAULT_IMAGE = "/images/default-workshop.png";

    public void setWorkshop(Workshop workshop) {
        nameLabel.setText(workshop.getName());
        descriptionLabel.setText(workshop.getDescription());
        priceLabel.setText(String.format("%.2f €", workshop.getPrice()));
        placesLabel.setText(String.valueOf(workshop.getAvailablePlaces()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        startDateLabel.setText(workshop.getStartsAt() != null ? dateFormat.format(workshop.getStartsAt()) : "N/A");

        // Gestion de l'image
        String imagePath = workshop.getImage();
        Image image;
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath); // Direct path from database
            if (imageFile.exists()) {
                image = new Image(imageFile.toURI().toString());
            } else {
                System.err.println("Image file not found: " + imageFile.getAbsolutePath());
                image = new Image(getClass().getResourceAsStream(DEFAULT_IMAGE));
            }
        } else {
            image = new Image(getClass().getResourceAsStream(DEFAULT_IMAGE));
        }
        workshopImage.setImage(image);
    }
}