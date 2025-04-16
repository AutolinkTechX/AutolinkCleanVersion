package org.example.pidev.services;

import org.example.pidev.entities.List_article;
import org.example.pidev.entities.Article;
import org.example.pidev.entities.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanierService {

    private Connection connection;

    public PanierService(Connection connection) {
        this.connection = connection;
    }

    // Méthode pour récupérer le panier pour un utilisateur spécifique
    public List<List_article> getPanierForUser(int userId) {
        List<List_article> articlesPanier = new ArrayList<>();

        String query = "SELECT la.id, la.quantite, la.prix_unitaire, "
                + "a.id as article_id, a.nom, a.description, a.image, "
                + "u.id as user_id, u.name, u.last_name "
                + "FROM list_article la "
                + "JOIN article a ON la.article_id = a.id "
                + "JOIN user u ON la.user_id = u.id "
                + "WHERE la.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                List_article listArticle = new List_article();
                listArticle.setId(rs.getInt("id"));
                listArticle.setQuantite(rs.getInt("quantite"));
                listArticle.setPrixUnitaire(rs.getDouble("prix_unitaire"));

                Article article = new Article();
                article.setId(rs.getInt("article_id"));
                article.setNom(rs.getString("nom"));
                article.setDescription(rs.getString("description"));
                article.setImage(rs.getString("image"));
                listArticle.setArticle(article);

                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setName(rs.getString("name"));
                user.setLastName(rs.getString("last_name"));
                listArticle.setUser(user);

                articlesPanier.add(listArticle);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du panier: " + e.getMessage());
            e.printStackTrace();
        }

        return articlesPanier;
    }

    // Méthode pour passer une commande (vider le panier après la commande)
    public boolean passerCommande(int userId) {
        List<List_article> panier = getPanierForUser(userId);
        if (panier.isEmpty()) {
            System.out.println("Le panier est vide, impossible de passer la commande.");
            return false;
        }

        String query = "DELETE FROM list_article WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println(rowsAffected + " articles supprimés du panier.");
                return true;
            } else {
                System.out.println("Erreur lors de la suppression des articles du panier.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du passage de commande: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Méthode pour retirer un article du panier
    public boolean removeArticleFromPanier(int listArticleId) {
        String query = "DELETE FROM list_article WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, listArticleId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'article: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateArticleQuantity(int articleId, int newQuantity) {
        String query = "UPDATE list_article SET quantite = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, newQuantity);
            statement.setInt(2, articleId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Méthode pour vider le panier d'un utilisateur avant ou après le paiement
    public boolean viderPanier(int userId) throws SQLException {
        String query = "DELETE FROM list_article WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des articles du panier: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean addArticleToPanier(int userId, int articleId, double prixUnitaire) {
        // Vérifier d'abord si l'article est déjà dans le panier
        String checkQuery = "SELECT id, quantite FROM list_article WHERE user_id = ? AND article_id = ?";
        String updateQuery = "UPDATE list_article SET quantite = ? WHERE id = ?";
        String insertQuery = "INSERT INTO list_article (user_id, article_id, quantite, prix_unitaire) VALUES (?, ?, 1, ?)";

        try {
            // Vérifier si l'article existe déjà
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, articleId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Article existe déjà - mettre à jour la quantité
                    int existingId = rs.getInt("id");
                    int existingQty = rs.getInt("quantite");
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, existingQty + 1);
                        updateStmt.setInt(2, existingId);
                        return updateStmt.executeUpdate() > 0;
                    }
                } else {
                    // Nouvel article - l'ajouter au panier
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setInt(2, articleId);
                        insertStmt.setDouble(3, prixUnitaire);
                        return insertStmt.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout au panier: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // Méthode pour calculer le total du panier
    public double calculerTotalPanier(int userId) throws SQLException {
        double total = 0.0;
        String query = "SELECT SUM(la.quantite * a.prix) as total " +
                "FROM list_article la " +
                "JOIN article a ON la.article_id = a.id " +
                "WHERE la.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        }
        return total;
    }

    // Méthode pour récupérer les articles avec leurs détails complets
    public List<Map<String, Object>> getArticlesAvecDetails(int userId) throws SQLException {
        List<Map<String, Object>> articlesDetails = new ArrayList<>();

        String query = "SELECT la.id, a.id as article_id, a.nom, a.prix, la.quantite, (a.prix * la.quantite) as total_article " +
                "FROM list_article la " +
                "JOIN article a ON la.article_id = a.id " +
                "WHERE la.user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> details = new HashMap<>();
                details.put("id", rs.getInt("id"));
                details.put("article_id", rs.getInt("article_id"));
                details.put("nom", rs.getString("nom"));
                details.put("prix", rs.getDouble("prix"));
                details.put("quantite", rs.getInt("quantite"));
                details.put("total_article", rs.getDouble("total_article"));

                articlesDetails.add(details);
            }
        }
        return articlesDetails;
    }

    // Méthode pour vérifier le stock avant paiement
    public boolean verifierStockDisponible(int userId) throws SQLException {
        String query = "SELECT COUNT(*) as indisponibles " +
                "FROM list_article la " +
                "JOIN article a ON la.article_id = a.id " +
                "WHERE la.user_id = ? AND la.quantite > a.quantitestock";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("indisponibles") == 0;
            }
        }
        return false;
    }


    public int getTotalQuantity(int userId) throws SQLException {
        int totalQuantity = 0;
        String query = "SELECT SUM(quantite) as total FROM list_article WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                totalQuantity = rs.getInt("total");
            }
        }
        return totalQuantity;
    }


    public int getPanierCountForUser(int userId) {
        String query = "SELECT COUNT(*) FROM list_article WHERE user_id = ?";
        try {
            PreparedStatement st = connection.prepareStatement(query);
            st.setInt(1, userId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }
}
