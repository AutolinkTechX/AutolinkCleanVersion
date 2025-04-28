package org.example.pidev.services;

import org.example.pidev.entities.Article;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FavorieService {

    private final Connection connection = MyDatabase.getInstance().getMyConnection();
    private final ArticleService articleService = new ArticleService(); // Utilisation d'ArticleService pour récupérer les articles

    public void addToFavorites(Integer articleId, Integer userId) throws SQLException {
        String query = "INSERT INTO favorie (article_id, user_id, date_creation, date_expiration) VALUES (?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDate = now.plusHours(24); // Expire dans 24 heures

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            statement.setInt(2, userId);
            statement.setTimestamp(3, Timestamp.valueOf(now));
            statement.setTimestamp(4, Timestamp.valueOf(expirationDate));
            statement.executeUpdate();
        }
    }

    public void removeFromFavorites(Integer articleId, Integer userId) throws SQLException {
        String query = "DELETE FROM favorie WHERE article_id = ? AND user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    public List<Article> getFavoriteArticlesByUser(int userId) throws SQLException {
        List<Article> favoriteArticles = new ArrayList<>();
        // Modifiez la requête pour joindre la table article et filtrer par quantité > 0
        String query = "SELECT f.article_id FROM favorie f " +
                "JOIN article a ON f.article_id = a.id " +
                "WHERE f.user_id = ? AND f.date_expiration > NOW() AND a.quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int articleId = resultSet.getInt("article_id");
                    Article article = articleService.getById(articleId);
                    if (article != null) {
                        favoriteArticles.add(article);
                    }
                }
            }
        }

        // Supprimer automatiquement les favoris pour les articles épuisés
        removeOutOfStockFavorites(userId);

        return favoriteArticles;
    }

    private void removeOutOfStockFavorites(int userId) throws SQLException {
        String query = "DELETE f FROM favorie f " +
                "JOIN article a ON f.article_id = a.id " +
                "WHERE f.user_id = ? AND a.quantitestock <= 0";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        }
    }

    public int getFavoriteCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie f " +
                "JOIN article a ON f.article_id = a.id " +
                "WHERE f.user_id = ? AND f.date_expiration > NOW() " +
                "AND a.quantitestock > 0";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean isArticleInFavorites(Integer articleId, Integer userId) throws SQLException {
        String query = "SELECT 1 FROM favorie f " +
                "JOIN article a ON f.article_id = a.id " +
                "WHERE f.article_id = ? AND f.user_id = ? " +
                "AND f.date_expiration > NOW() AND a.quantitestock > 0 " +
                "LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            statement.setInt(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<String> getAllCategories() throws SQLException {
        return articleService.getAllCategories(); // Utiliser la méthode d'ArticleService pour obtenir les catégories
    }

    public int getFavorisCountForUser(int userId) {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ?";
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

    public void deleteByArticleId(int articleId) throws SQLException {
        String sql = "DELETE FROM favorie WHERE article_id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, articleId);
            stmt.executeUpdate();
        }
    }
}
