package org.example.pidev.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.pidev.entities.Commande;
import org.example.pidev.entities.User;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  /*
    public List<Commande> getAllCommandes(int page, int itemsPerPage) {
        List<Commande> commandes = new ArrayList<>();
        int offset = (page - 1) * itemsPerPage;

        String query = "SELECT c.*, u.name, u.last_name FROM commande c " +
                "JOIN user u ON c.client_id = u.id " +
                "ORDER BY c.date_commande DESC LIMIT ? OFFSET ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, itemsPerPage);
            stmt.setInt(2, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                commandes.add(mapResultSetToCommande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commandes;
    }
*/
  public List<Commande> getAllCommandes(int page, int itemsPerPage) {
      List<Commande> commandes = new ArrayList<>();
      int offset = (page - 1) * itemsPerPage;

      String query = "SELECT c.*, u.name, u.last_name, u.email FROM commande c " + // Ajout de u.email
              "JOIN user u ON c.client_id = u.id " +
              "ORDER BY c.date_commande DESC LIMIT ? OFFSET ?";

      try (PreparedStatement stmt = connection.prepareStatement(query)) {
          stmt.setInt(1, itemsPerPage);
          stmt.setInt(2, offset);

          ResultSet rs = stmt.executeQuery();
          while (rs.next()) {
              commandes.add(mapResultSetToCommande(rs));
          }
      } catch (SQLException e) {
          e.printStackTrace();
      }
      return commandes;
  }

    public List<Commande> getCommandesByDate(LocalDate date, int page, int itemsPerPage) {
        List<Commande> commandes = new ArrayList<>();
        int offset = (page - 1) * itemsPerPage;

        String query = "SELECT c.*, u.name, u.last_name FROM commande c " +
                "JOIN user u ON c.client_id = u.id " +
                "WHERE DATE(c.date_commande) = ? " +
                "ORDER BY c.date_commande DESC LIMIT ? OFFSET ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setInt(2, itemsPerPage);
            stmt.setInt(3, offset);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                commandes.add(mapResultSetToCommande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commandes;
    }

    public int getCommandesCount() {
        String query = "SELECT COUNT(*) FROM commande";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCommandesCountByDate(LocalDate date) {
        String query = "SELECT COUNT(*) FROM commande WHERE DATE(date_commande) = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Commande mapResultSetToCommande(ResultSet rs) throws SQLException {
        Commande commande = new Commande();
        commande.setId(rs.getInt("id"));
        commande.setDateCommande(rs.getTimestamp("date_commande").toLocalDateTime());
        commande.setModePaiement(rs.getString("mode_paiement"));
        commande.setTotal(rs.getDouble("total"));

        User client = new User();
        client.setId(rs.getInt("client_id"));
        client.setName(rs.getString("name"));
        client.setLastName(rs.getString("last_name"));
        client.setEmail(rs.getString("email"));
        commande.setClient(client);

        commande.setArticleIds(rs.getString("article_ids"));
        commande.setQuantites(rs.getString("quantites"));

        return commande;
    }

    public Map<String, Long> getPaymentMethodStatistics() throws SQLException {
        Map<String, Long> paymentStats = new HashMap<>();
        String query = "SELECT mode_paiement, COUNT(*) as count FROM commande GROUP BY mode_paiement";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                paymentStats.put(rs.getString("mode_paiement"), rs.getLong("count"));
            }
        }

        // Valeurs par défaut si la table est vide
        if (paymentStats.isEmpty()) {
            paymentStats.put("card", 65L);
            paymentStats.put("espece", 35L);
        }

        return paymentStats;
    }

    public Map<LocalDate, List<String>> getCommandesGroupedByDateWithClientNames() throws SQLException {
        Map<LocalDate, List<String>> commandesByDate = new HashMap<>();

        String sql = "SELECT DATE(c.date_commande) as commande_date, " +
                "CONCAT(cl.last_name, ' ', cl.name) as client_name " +
                "FROM commande c " +
                "JOIN user cl ON c.client_id = cl.id " +
                "ORDER BY c.date_commande DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("commande_date").toLocalDate();
                String clientName = rs.getString("client_name");

                commandesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(clientName);
            }
        }

        return commandesByDate;
    }

    /*
    public List<Commande> getAllCommandees() {
        try {
            List<Commande> commandes = new ArrayList<>();
            String query = "SELECT c.id, c.date_commande, c.total, u.name as client_name, u.last_name " +
                    "FROM commande c " +
                    "JOIN user u ON c.client_id = u.id ";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    Commande commande = new Commande();
                    commande.setId(resultSet.getInt("id"));

                    Timestamp timestamp = resultSet.getTimestamp("date_commande");
                    if (timestamp != null) {
                        commande.setDateCommande(timestamp.toLocalDateTime());
                    }

                    User client = new User();
                    client.setName(resultSet.getString("client_name"));
                    client.setLastName(resultSet.getString("last_name"));
                    commande.setClient(client);

                    commande.setTotal(resultSet.getDouble("total"));

                    commandes.add(commande);
                }
            }
            return commandes;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve commandes", e);
        }
    }
*/

    public List<Commande> getAllCommandees() {
        try {
            List<Commande> commandes = new ArrayList<>();
            String query = "SELECT c.id, c.date_commande, c.total, " +
                    "u.name as client_name, u.last_name, u.email " + // Ajout de u.email
                    "FROM commande c " +
                    "JOIN user u ON c.client_id = u.id ";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    Commande commande = new Commande();
                    commande.setId(resultSet.getInt("id"));

                    Timestamp timestamp = resultSet.getTimestamp("date_commande");
                    if (timestamp != null) {
                        commande.setDateCommande(timestamp.toLocalDateTime());
                    }

                    User client = new User();
                    client.setName(resultSet.getString("client_name"));
                    client.setLastName(resultSet.getString("last_name"));
                    client.setEmail(resultSet.getString("email")); // Ajout de l'email
                    commande.setClient(client);

                    commande.setTotal(resultSet.getDouble("total"));

                    commandes.add(commande);
                }
            }
            return commandes;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve commandes", e);
        }
    }

    private Commande mapResultSetToCommandee(ResultSet resultSet) throws SQLException {
        Commande commande = new Commande();
        commande.setId(resultSet.getInt("id"));

        Timestamp timestamp = resultSet.getTimestamp("date_commande");
        LocalDateTime dateCommande = timestamp != null ? timestamp.toLocalDateTime() : null;
        commande.setDateCommande(dateCommande);

        User client = new User();
        client.setName(resultSet.getString("client_name"));
        commande.setClient(client);

        commande.setTotal(resultSet.getDouble("total"));

        return commande;
    }
}