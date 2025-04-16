package org.example.pidev.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pidev.entities.Commande;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CommandeService {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public CommandeService(Connection connection) {
        this.connection = connection;
        this.objectMapper = new ObjectMapper();
    }

    // Méthode améliorée pour désérialiser les quantités
    public Map<Integer, Integer> deserializeQuantites(String quantitesJson) throws SQLException {
        try {
            if (quantitesJson == null || quantitesJson.trim().isEmpty()) {
                return new HashMap<>(); // Retourne une Map vide si le JSON est vide
            }
            return objectMapper.readValue(quantitesJson, new TypeReference<Map<Integer, Integer>>() {});
        } catch (Exception e) {
            throw new SQLException("Erreur lors de la désérialisation des quantités : " + e.getMessage(), e);
        }
    }

    // Méthode pour récupérer une commande avec gestion robuste des quantités
    public Commande getCommandeById(int id) throws SQLException {
        String query = "SELECT * FROM commande WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Commande commande = new Commande();
                commande.setId(resultSet.getInt("id"));
                commande.setDateCommande(resultSet.getTimestamp("date_commande").toLocalDateTime());
                commande.setModePaiement(resultSet.getString("mode_paiement"));
                commande.setTotal(resultSet.getDouble("total"));

                // Gestion robuste des champs JSON
                String quantitesJson = resultSet.getString("quantites");
                if (quantitesJson == null) quantitesJson = "{}";
                commande.setQuantites(quantitesJson);

                String articleIdsJson = resultSet.getString("article_ids");
                if (articleIdsJson == null) articleIdsJson = "{}";
                commande.setArticleIds(articleIdsJson);

                return commande;
            }
            return null;
        }
    }

    // Méthode pour créer une commande avec validation des données
    public Commande createCommande(Commande commande) throws SQLException {
        String query = "INSERT INTO commande (client_id, date_commande, mode_paiement, total, article_ids, quantites) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        // Validation des champs JSON
        if (commande.getQuantites() == null || commande.getQuantites().trim().isEmpty()) {
            commande.setQuantites("{}");
        }
        if (commande.getArticleIds() == null || commande.getArticleIds().trim().isEmpty()) {
            commande.setArticleIds("{}");
        }

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, commande.getClient().getId());
            statement.setObject(2, commande.getDateCommande());
            statement.setString(3, commande.getModePaiement());
            statement.setDouble(4, commande.getTotal());
            statement.setString(5, commande.getArticleIds());
            statement.setString(6, commande.getQuantites());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Échec de la création de la commande, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    commande.setId(generatedKeys.getInt(1));
                    return commande;
                } else {
                    throw new SQLException("Échec de la création de la commande, aucun ID généré.");
                }
            }
        }
    }

    // Méthode utilitaire pour sérialiser les quantités
    public String serializeQuantites(Map<Integer, Integer> quantitesMap) throws SQLException {
        try {
            return objectMapper.writeValueAsString(quantitesMap != null ? quantitesMap : new HashMap<>());
        } catch (Exception e) {
            throw new SQLException("Erreur lors de la sérialisation des quantités : " + e.getMessage(), e);
        }
    }
}