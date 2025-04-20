package org.example.pidev.controllers;

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
import org.example.pidev.entities.Entreprise;
import org.example.pidev.services.ServiceAccord;
import org.example.pidev.services.ServiceMaterielRecyclable;
import org.example.pidev.utils.SessionManager;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
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
            afficherBarChartAttente();
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

    private void afficherPieChartStatut() throws SQLException {
        int idEntrepriseConnectee = getEntrepriseConnecteeId(); // méthode fictive à définir ou variable à utiliser
        Map<String, Integer> data = serviceA.getNombreDemandesParStatut(idEntrepriseConnectee);

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = entry.getKey() + " (" + entry.getValue() + ")";
            chartData.add(new PieChart.Data(label, entry.getValue()));
        }

        pieChartStatut.setData(chartData);
        pieChartStatut.setTitle("Répartition par Statut");
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
                node.setStyle("-fx-bar-fill: #2196F3;");
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

    private void afficherBarChartAttente() {
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
    }

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
        // Masquer le bouton avant de prendre la capture
        telechargerButton.setVisible(false);

        // Prendre un snapshot de tout le conteneur de la page (sans le bouton)
        WritableImage image = statistiquesPane.snapshot(new SnapshotParameters(), null);

        // Générer le nom de fichier avec la date du jour
        String date = java.time.LocalDate.now().toString(); // format YYYY-MM-DD
        String fileName = "statistiques-" + date + ".png";

        // Créer un objet FileChooser pour choisir l'emplacement d'enregistrement
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer les statistiques");
        fileChooser.setInitialFileName(fileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));

        // Afficher la boîte de dialogue pour choisir l'emplacement
        File file = fileChooser.showSaveDialog(statistiquesPane.getScene().getWindow());

        if (file != null) {
            // Vérifier si un fichier avec le même nom existe déjà
            File parentDirectory = file.getParentFile();
            String baseName = file.getName();
            int i = 1;
            while (new File(parentDirectory, baseName).exists()) {
                // Si le fichier existe déjà, ajouter un suffixe numérique au nom du fichier
                baseName = fileName.substring(0, fileName.lastIndexOf("."))
                        + "(" + i + ").png";
                i++;
            }

            // Enregistrer l'image avec le nom généré
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File(parentDirectory, baseName));

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Téléchargement réussi");
                alert.setHeaderText(null);
                alert.setContentText("La page des statistiques a été enregistrée sous :\n" + baseName);
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText("Échec de l'enregistrement");
                error.setContentText("Impossible d'enregistrer l'image : " + e.getMessage());
                error.showAndWait();
            }
        }

        // Rendre le bouton visible après la capture
        telechargerButton.setVisible(true);
    }
}
