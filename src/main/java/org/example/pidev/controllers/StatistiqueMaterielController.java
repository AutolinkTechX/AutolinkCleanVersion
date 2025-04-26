package org.example.pidev.controllers;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.ServiceAccord;
import org.example.pidev.services.ServiceMaterielRecyclable;
import org.example.pidev.utils.SessionManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class StatistiqueMaterielController implements Initializable {

    @FXML
    private AnchorPane statistiquesPane;

    @FXML
    private Button telechargerButton;  // L'ajout du fx:id pour le bouton
    @FXML
    private Label totalDemandesLabel;

    @FXML
    private Label tempsMoyenLabel;

    @FXML
    private PieChart pieChartStatut;

    @FXML
    private PieChart pieChartType;

    @FXML
    private BarChart<String, Number> barChartDemandes;

    @FXML
    private BarChart<String, Number> barChartAttente;

    private ServiceMaterielRecyclable service = new ServiceMaterielRecyclable();
    private ServiceAccord serviceA=new ServiceAccord();


    /*hedhi zedtha bch najem naayet lel controlleur fi dashboardEntreprise*/
    private Entreprise entreprise;

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
        try {
            afficherTotalDemandes();
            afficherTempsMoyenTraitement();
            afficherPieChartStatut();
            /*afficherPieChartType();*/
            afficherBarChartDemandes();
           /* afficherBarChartAttente();*/
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private int getEntrepriseConnecteeId() {
        if (SessionManager.getCurrentEntreprise() != null) {
            return SessionManager.getCurrentEntreprise().getId();
        } else {
            throw new IllegalStateException("Aucune entreprise connectée !");
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // L'initialisation des graphiques sera faite après que l'entreprise soit définie
    }

    private void afficherTotalDemandes() throws SQLException {
        int entrepriseId = getEntrepriseConnecteeId(); // récupère l'entreprise connectée
        int total = serviceA.getNombreDemandesParEntreprise(entrepriseId); // appels personnalisés
        totalDemandesLabel.setText("Nombre de demandes de votre entreprise : " + total);
    }


    private void afficherTempsMoyenTraitement() throws SQLException {
        double tempsMoyen = serviceA.getTempsMoyenTraitement(entreprise.getId());
        // Convertir les heures en jours et heures
        int jours = (int) (tempsMoyen / 24);
        int heures = (int) (tempsMoyen % 24);

        String tempsFormate = String.format("%d jour%s %d heure%s",
                jours, jours > 1 ? "s" : "",
                heures, heures > 1 ? "s" : "");

        tempsMoyenLabel.setText("Temps moyen de traitement : " + tempsFormate);
    }

    /*private void afficherPieChartStatut() throws SQLException {
        int idEntrepriseConnectee = getEntrepriseConnecteeId(); // méthode fictive à définir ou variable à utiliser
        Map<String, Integer> data = serviceA.getNombreDemandesParStatut(idEntrepriseConnectee);

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = entry.getKey() + " (" + entry.getValue() + ")";
            chartData.add(new PieChart.Data(label, entry.getValue()));
        }

        pieChartStatut.setData(chartData);
        pieChartStatut.setTitle("Répartition par Statut");
    }*/

    private void afficherPieChartStatut() throws SQLException {
        int idEntrepriseConnectee = getEntrepriseConnecteeId();
        Map<String, Integer> data = serviceA.getNombreDemandesParStatut(idEntrepriseConnectee);

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        // Création des données du PieChart
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = entry.getKey() + " (" + entry.getValue() + ")";
            chartData.add(new PieChart.Data(label, entry.getValue()));
        }

        // Affecter les données sans redéfinir de couleurs
        pieChartStatut.setData(chartData);

        // ⚠️ Ne rien faire ici pour que JavaFX garde ses couleurs automatiques pour les slices et la légende
    }



   /* private void afficherPieChartType() throws SQLException {
        Map<String, Integer> data = service.getNombreDemandesParType();
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            chartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        pieChartType.setData(chartData);
        pieChartType.setTitle("Répartition par Type de Matériel");
    }*/

    private void afficherBarChartDemandes() {
        try {
            Map<String, Integer> demandesParClient = serviceA.getNombreDemandesParClient(entreprise.getId());

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Nombre de demandes par client");

            for (Map.Entry<String, Integer> entry : demandesParClient.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            barChartDemandes.getData().add(series);

            // Personnalisation de l'apparence
            barChartDemandes.setTitle("Nombre de demandes par client");
            barChartDemandes.setLegendVisible(false);

            // Personnalisation des couleurs des barres
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                node.setStyle("-fx-bar-fill: #d17942;");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Afficher une alerte d'erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors du chargement des données");
            alert.setContentText("Une erreur est survenue lors du chargement des données : " + e.getMessage());
            alert.showAndWait();
        }
    }

    /*private void afficherBarChartAttente() {
        try {
            Map<String, Long> materiauxEnAttente = serviceA.getMateriauxEnAttenteLongue(entreprise.getId());

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Temps d'attente (heures)");

            for (Map.Entry<String, Long> entry : materiauxEnAttente.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            barChartAttente.getData().add(series);

            // Personnalisation de l'apparence
            barChartAttente.setTitle("Matériaux en attente depuis plus de 24h");
            barChartAttente.setLegendVisible(false);

            // Personnalisation des couleurs des barres
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                node.setStyle("-fx-bar-fill: #FF5722;"); // Orange pour indiquer l'urgence
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors du chargement des données");
            alert.setContentText("Une erreur est survenue lors du chargement des données : " + e.getMessage());
            alert.showAndWait();
        }
    }*/

   /*private void afficherEvolutionDemandesParMois() throws SQLException {
        Map<String, Integer> data = serviceA.getNombreDemandesParMois();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Demandes par Mois");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        lineChartDemandes.getData().clear(); // lineChartDemandes est le fx:id du LineChart dans ton FXML
        lineChartDemandes.getData().add(series);
    }
*/




    @FXML
    private void telechargerCapture() {
        try {
            telechargerButton.setVisible(false);

            // 1. Capture du panneau avec meilleure résolution
            statistiquesPane.setScaleX(1.5);
            statistiquesPane.setScaleY(1.5);
            WritableImage snapshot = statistiquesPane.snapshot(new SnapshotParameters(), null);
            statistiquesPane.setScaleX(1);
            statistiquesPane.setScaleY(1);

            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

            // 2. Conversion en byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // 3. Sélection de l'emplacement de sauvegarde
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.setInitialFileName("statistiques-" + LocalDate.now() + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

            Window window = statistiquesPane.getScene() != null ? statistiquesPane.getScene().getWindow() : null;
            File file = fileChooser.showSaveDialog(window);

            if (file == null) return;

            // 4. Création du PDF avec les nouvelles modifications
            try (PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc, new PageSize(1400, 900))) {

                document.setMargins(50, 50, 50, 50);

                // === NOUVEL EN-TÊTE À 3 COLONNES ===
                Table headerTable = new Table(new float[]{30f, 40f, 30f});
                headerTable.setWidth(UnitValue.createPercentValue(100));

                // Colonne gauche - Contact
                Paragraph contact = new Paragraph()
                        .add(new Text("Autol.Ink\n").setBold().setFontSize(20))
                        .add("123 Rue de l'Innovation\nTunis, Tunisie\n")
                        .add("Tél: +216 48 004 881\n")
                        .add("Email: contact@autol.ink.com\n")
                        .add("Site: www.autol.ink.com")
                        .setFontColor(ColorConstants.DARK_GRAY);

                headerTable.addCell(new Cell()
                        .add(contact)
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.LEFT));

                // Colonne centrale - NOUVEAU TITRE
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm");
                String dateTime = LocalDateTime.now().format(formatter);

                Paragraph title = new Paragraph("Statistiques  d'aujourd'hui\n" + dateTime)
                        .setBold()
                        .setFontSize(22)
                        .setTextAlignment(TextAlignment.CENTER)
                        //  .setFontColor(ColorConstants.BLUE)
                        .setFontColor(new DeviceRgb(0xB5, 0x60, 0x2C)) // Couleur terre cuite #b5602c

                        .setMarginTop(10);

                headerTable.addCell(new Cell()
                        .add(title)
                        .setBorder(Border.NO_BORDER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setTextAlignment(TextAlignment.CENTER));

                // Colonne droite - Logo
                URL logoUrl = getClass().getResource("/icons/logo.png");
                if (logoUrl == null) {
                    throw new IOException("Logo non trouvé. Vérifiez: /icons/logo.png dans target/classes/");
                }

                byte[] logoBytes;
                try (InputStream logoStream = logoUrl.openStream()) {
                    BufferedImage originalImage = ImageIO.read(logoStream);
                    ByteArrayOutputStream logoBaos = new ByteArrayOutputStream();
                    ImageIO.write(originalImage, "PNG", logoBaos);
                    logoBytes = logoBaos.toByteArray();
                }

                ImageData logoData = ImageDataFactory.create(logoBytes);
                com.itextpdf.layout.element.Image logo = new com.itextpdf.layout.element.Image(logoData);
                logo.scaleToFit(120, 120)
                        .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                        .setMarginRight(15);

                headerTable.addCell(new Cell()
                        .add(logo)
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.RIGHT));

                document.add(headerTable);

                // === IMAGE DES STATISTIQUES ===
                ImageData statsImageData = ImageDataFactory.create(imageBytes);
                com.itextpdf.layout.element.Image statsImage = new com.itextpdf.layout.element.Image(statsImageData);

                statsImage.scaleToFit(3000, 600)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginTop(30)
                        .setMarginBottom(20);

                document.add(statsImage);

            }

            showAlert(Alert.AlertType.INFORMATION, "PDF généré", "Enregistrement réussi !", "");

        } catch (Exception e) {
            handleError(e);
        } finally {
            telechargerButton.setVisible(true);
        }
    }
    // Méthodes utilitaires
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleError(Exception e) {
        String errorDetails = "Erreur technique : \n" + e.getClass().getSimpleName() + "\n" + e.getMessage();
        System.err.println(errorDetails);
        e.printStackTrace();

        showAlert(Alert.AlertType.ERROR,
                "Erreur",
                "Échec de la génération PDF",
                (e instanceof IOException) ?
                        "Problème de fichier ou ressource manquante" :
                        "Veuillez contacter le support technique"
        );
    }
}
