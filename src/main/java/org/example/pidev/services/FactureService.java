package org.example.pidev.services;

import org.example.pidev.entities.Article;
import org.example.pidev.entities.Facture;
import org.example.pidev.entities.Commande;
import org.example.pidev.entities.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactureService {

    private Connection connection;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FactureService(Connection connection) {
        this.connection = connection;
    }

    // Méthode pour ajouter une facture
    public void ajouterFacture(Facture facture) throws SQLException {
        String query = "INSERT INTO facture (montant, datetime, commande_id, client_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDouble(1, facture.getMontant());
            statement.setTimestamp(2, Timestamp.valueOf(facture.getDatetime()));
            statement.setInt(3, facture.getCommande().getId());
            statement.setInt(4, facture.getClient().getId());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Échec de la création de la facture, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    facture.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Échec de la création de la facture, aucun ID obtenu.");
                }
            }
        }
    }

    // Méthode pour obtenir les factures d'un utilisateur
    public List<Facture> obtenirFacturesParUtilisateur(int userId) throws SQLException {
        List<Facture> factures = new ArrayList<>();
        String query = "SELECT f.*, c.id as commande_id, u.name as client_nom, u.last_name as client_prenom, u.email as client_email " +
                "FROM facture f " +
                "LEFT JOIN commande c ON f.commande_id = c.id " +
                "LEFT JOIN user u ON f.client_id = u.id " +
                "WHERE f.client_id = ? " +
                "ORDER BY f.datetime DESC";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Facture facture = mapResultSetToFacture(resultSet);
                    if (facture != null) {
                        factures.add(facture);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des factures: " + e.getMessage());
            throw e;
        }

        return factures;
    }

    // Méthode pour rechercher des factures par date
    public List<Facture> rechercherFacturesParDate(LocalDateTime date, int userId) throws SQLException {
        List<Facture> factures = new ArrayList<>();
        String query = "SELECT * FROM facture WHERE client_id = ? AND DATE(datetime) = DATE(?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setTimestamp(2, Timestamp.valueOf(date));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    factures.add(mapResultSetToFacture(resultSet));
                }
            }
        }
        return factures;
    }

 /*   // Méthode pour télécharger une facture en PDF
    public boolean downloadFacture(int factureId) throws SQLException, IOException {
        Facture facture = getFactureById(factureId);
        if (facture == null) {
            return false;
        }

        byte[] pdfContent = generatePdf(facture);
        String fileName = "Facture_" + factureId + "_" +
                facture.getDatetime().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        Path path = Paths.get(System.getProperty("user.home"), "Downloads", fileName);

        Files.write(path, pdfContent);
        return true;
    }
*/
    // Méthode pour obtenir une facture par son ID
    public Facture getFactureById(int id) throws SQLException {
        String query = "SELECT f.*, c.id as commande_id, u.name as client_nom, u.last_name as client_prenom, u.email as client_email " +
                "FROM facture f " +
                "LEFT JOIN commande c ON f.commande_id = c.id " +
                "LEFT JOIN user u ON f.client_id = u.id " +
                "WHERE f.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToFacture(resultSet);
                }
            }
        }
        return null;
    }

 /*   // Méthode pour générer le PDF d'une facture
    private byte[] generatePdf(Facture facture) throws SQLException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Titre
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            Paragraph title = new Paragraph("FACTURE")
                    .setFont(titleFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // Informations de la facture
            float[] columnWidths = {1, 2};
            Table infoTable = new Table(columnWidths);
            infoTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
            infoTable.setMarginBottom(20);

            addInfoCell(infoTable, "N° Facture:", String.valueOf(facture.getId()));
            addInfoCell(infoTable, "Date:", facture.getDatetime().format(dateFormatter));
            addInfoCell(infoTable, "Client:",
                    facture.getClient().getName() + " " + facture.getClient().getLastName());
            addInfoCell(infoTable, "Commande:", "N°" + facture.getCommande().getId());

            document.add(infoTable);

            // Montant
            PdfFont amountFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            Paragraph amount = new Paragraph(
                    "Montant total: " + String.format("%.2f", facture.getMontant()) + " DT")
                    .setFont(amountFont)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20);
            document.add(amount);
        }

        return outputStream.toByteArray();
    }

    // Méthode utilitaire pour ajouter une cellule au tableau PDF
    private void addInfoCell(jakarta.persistence.Table table, String label, String value) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        Cell labelCell = new Cell().add(new Paragraph(label).setFont(boldFont));
        labelCell.setBorder(null);

        Cell valueCell = new Cell().add(new Paragraph(value).setFont(normalFont));
        valueCell.setBorder(null);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
*/
    // Méthode pour mapper un ResultSet à une entité Facture
    private Facture mapResultSetToFacture(ResultSet resultSet) throws SQLException {
        try {
            Facture facture = new Facture();
            facture.setId(resultSet.getInt("id"));
            facture.setMontant(resultSet.getDouble("montant"));
            facture.setDatetime(resultSet.getTimestamp("datetime").toLocalDateTime());

            // Gestion de la commande (peut être null)
            if (resultSet.getObject("commande_id") != null) {
                Commande commande = new Commande();
                commande.setId(resultSet.getInt("commande_id"));
                facture.setCommande(commande);
            }

            // Gestion du client (ne devrait pas être null)
            User client = new User();
            client.setId(resultSet.getInt("client_id"));
            client.setName(resultSet.getString("client_nom"));
            client.setLastName(resultSet.getString("client_prenom"));
            client.setEmail(resultSet.getString("client_email")); // Ajout de l'email
            facture.setClient(client);

            return facture;
        } catch (SQLException e) {
            System.err.println("Erreur lors du mapping de la facture: " + e.getMessage());
            throw e;
        }
    }

    // Méthode pour supprimer une facture
    public boolean supprimerFacture(int id) throws SQLException {
        String query = "DELETE FROM facture WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }


    public Map<Article, Integer> getArticlesWithQuantitiesForCommande(int commandeId) {
        String query = "SELECT a.*, ca.quantite FROM article a " +
                "JOIN commande_article ca ON a.id = ca.article_id " +
                "WHERE ca.commande_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, commandeId);
            ResultSet rs = stmt.executeQuery();

            Map<Article, Integer> articles = new HashMap<>();
            while (rs.next()) {
                Article article = new Article();
                article.setId(rs.getInt("id"));
                article.setNom(rs.getString("nom"));
                article.setPrix(rs.getDouble("prix"));
                // ... autres propriétés ...

                int quantite = rs.getInt("quantite");
                articles.put(article, quantite);
            }
            return articles;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des articles de la commande", e);
        }
    }




}