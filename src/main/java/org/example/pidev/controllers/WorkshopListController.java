package org.example.pidev.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.pidev.entities.Workshop;
import org.example.pidev.services.ServiceWorkshop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WorkshopListController {

    @FXML
    private Button backButton;
    @FXML
    private Button generatePdfButton;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane workshopFlowPane;

    private final ServiceWorkshop workshopService = new ServiceWorkshop();
    private ObservableList<Workshop> allWorkshops = FXCollections.observableArrayList();

    // --- Fonts for PDF ---
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BaseColor.WHITE);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
    private static final Font TABLE_BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
    private static final BaseColor HEADER_BACKGROUND = new BaseColor(77, 120, 181);
    private static final BaseColor TABLE_HEADER_BACKGROUND = new BaseColor(50, 80, 120);

    @FXML
    private void initialize() {
        loadAllWorkshops();
        setupSearch();
    }

    private void setupSearch() {
        // Search functionality can be implemented here if needed
    }

    private void loadAllWorkshops() {
        try {
            List<Workshop> workshops = workshopService.getAllWorkshops();
            allWorkshops.setAll(workshops);
            updateWorkshopCards(allWorkshops);
            generatePdfButton.setDisable(allWorkshops.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de chargement", "Impossible de charger les workshops: " + e.getMessage());
        }
    }

    private void updateWorkshopCards(ObservableList<Workshop> workshops) {
        workshopFlowPane.getChildren().clear();

        for (Workshop workshop : workshops) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopCard.fxml"));
                VBox card = loader.load();
                WorkshopCardController controller = loader.getController();
                controller.setWorkshop(workshop);

                card.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        showWorkshopDetails(workshop);
                    }
                });

                workshopFlowPane.getChildren().add(card);

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la carte du workshop: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGeneratePdf() {
        Workshop selectedWorkshop = getSelectedWorkshop();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));

        if (selectedWorkshop != null) {
            fileChooser.setInitialFileName(selectedWorkshop.getName() + "_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");
            File file = fileChooser.showSaveDialog(workshopFlowPane.getScene().getWindow());
            if (file != null) {
                generateSingleWorkshopPdf(file, selectedWorkshop);
            }
        } else {
            fileChooser.setInitialFileName("liste_workshops_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf");
            File file = fileChooser.showSaveDialog(workshopFlowPane.getScene().getWindow());
            if (file != null) {
                generateAllWorkshopsPdf(file);
            }
        }
    }

    private void generateSingleWorkshopPdf(File file, Workshop workshop) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Metadata
            document.addTitle("Workshop : " + workshop.getName());
            document.addAuthor("Votre Application");
            document.addCreator("Votre Application avec iText");

            // Title
            Paragraph title = new Paragraph("Détails du Workshop", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Workshop Image
            String imagePath = workshop.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Paragraph imageTitle = new Paragraph("Image du Workshop", BODY_FONT);
                    imageTitle.setSpacingAfter(10f);
                    document.add(imageTitle);

                    com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(imagePath);
                    image.scaleToFit(300, 200);
                    image.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                    document.add(image);
                    document.add(Chunk.NEWLINE);
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                    document.add(new Paragraph("Image non disponible", BODY_FONT));
                }
            }

            // Workshop Details Table
            PdfPTable pdfTable = new PdfPTable(2);
            pdfTable.setWidthPercentage(80);
            pdfTable.setSpacingBefore(20f);

            addTableRow(pdfTable, "Nom", workshop.getName());
            addTableRow(pdfTable, "Description", workshop.getDescription());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            addTableRow(pdfTable, "Date de début", workshop.getStartsAt() != null ? dateFormat.format(workshop.getStartsAt()) : "N/A");
            addTableRow(pdfTable, "Date de fin", workshop.getEndsAt() != null ? dateFormat.format(workshop.getEndsAt()) : "N/A");
            addTableRow(pdfTable, "Lieu", workshop.getLocation());
            addTableRow(pdfTable, "Prix (€)", String.format("%.2f", workshop.getPrice()));
            addTableRow(pdfTable, "Places disponibles", String.valueOf(workshop.getAvailablePlaces()));

            document.add(pdfTable);
            document.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Généré", "Le fichier PDF a été enregistré avec succès:\n" + file.getAbsolutePath());

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur PDF", "Impossible de générer le fichier PDF : " + e.getMessage());
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void generateAllWorkshopsPdf(File file) {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.addTitle("Liste des Workshops");
            document.addAuthor("Votre Application");
            document.addCreator("Votre Application avec iText");

            Paragraph title = new Paragraph("Liste des Workshops", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            Paragraph dateParagraph = new Paragraph("Généré le: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), BODY_FONT);
            dateParagraph.setAlignment(Element.ALIGN_RIGHT);
            dateParagraph.setSpacingAfter(15f);
            document.add(dateParagraph);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            addTableHeaderCell(table, "Nom");
            addTableHeaderCell(table, "Description");
            addTableHeaderCell(table, "Date début");
            addTableHeaderCell(table, "Date fin");
            addTableHeaderCell(table, "Lieu");
            addTableHeaderCell(table, "Prix (€)");
            addTableHeaderCell(table, "Places");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            for (Workshop workshop : allWorkshops) {
                addTableCell(table, workshop.getName());
                addTableCell(table, workshop.getDescription());
                addTableCell(table, workshop.getStartsAt() != null ? dateFormat.format(workshop.getStartsAt()) : "N/A");
                addTableCell(table, workshop.getEndsAt() != null ? dateFormat.format(workshop.getEndsAt()) : "N/A");
                addTableCell(table, workshop.getLocation());
                addTableCell(table, String.format("%.2f", workshop.getPrice()));
                addTableCell(table, String.valueOf(workshop.getAvailablePlaces()));
            }

            document.add(table);
            document.close();

            showAlert(Alert.AlertType.INFORMATION, "PDF Généré", "La liste des workshops a été exportée avec succès:\n" + file.getAbsolutePath());

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur PDF", "Impossible de générer le fichier PDF : " + e.getMessage());
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addTableHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(TABLE_HEADER_BACKGROUND);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_BODY_FONT));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBackgroundColor(HEADER_BACKGROUND);
        labelCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", BODY_FONT));
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Workshops");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de Navigation", "Impossible de retourner à la vue précédente: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        if (keyword.isEmpty()) {
            allWorkshops.setAll(workshopService.getAllWorkshops());
        } else {
            List<Workshop> filtered = workshopService.getAllWorkshops().stream()
                    .filter(w -> !w.getName().isEmpty() &&
                            w.getName().toLowerCase().startsWith(keyword))
                    .collect(Collectors.toList());
            allWorkshops.setAll(filtered);
        }
        updateWorkshopCards(allWorkshops);
    }

    private void showWorkshopDetails(Workshop workshop) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_workshop/WorkshopDetailsView.fxml"));
            Parent root = loader.load();

            WorkshopDetailsController controller = loader.getController();
            controller.setWorkshop(workshop);

            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setWorkshopData(workshop, (Stage) workshopFlowPane.getScene().getWindow());

            stage.setScene(new Scene(root));
            stage.setTitle("Détails du Workshop");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'afficher les détails du workshop.");
        }
    }

    private Workshop getSelectedWorkshop() {
        return null;
    }
}