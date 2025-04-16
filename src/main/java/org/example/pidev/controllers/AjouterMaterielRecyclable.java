package org.example.pidev.controllers;


import org.example.pidev.Enum.Type_materiel;

import javafx.fxml.FXML;

import javafx.scene.control.*;


public class AjouterMaterielRecyclable {


    @FXML

    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ComboBox<String> entrepriseComboBox;

    @FXML

    private ComboBox<Type_materiel> typeMaterielComboBox;

    @FXML
    private Button chooseImageButton;

    @FXML
    private Label imageLabel;

    @FXML
    private Button saveButton;
}
