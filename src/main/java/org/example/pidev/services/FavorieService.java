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

    // Ajouter un article aux favoris d'un utilisateur
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

    // Retirer un article des favoris d'un utilisateur
    public void removeFromFavorites(Integer articleId, Integer userId) throws SQLException {
        String query = "DELETE FROM favorie WHERE article_id = ? AND user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            statement.setInt(2, userId);
            statement.executeUpdate();
        }
    }

    // Récupérer tous les articles favoris d'un utilisateur

    public List<Article> getFavoriteArticlesByUser(int userId) throws SQLException {
        List<Article> favoriteArticles = new ArrayList<>();
        String query = "SELECT article_id FROM favorie WHERE user_id = ? AND date_expiration > NOW()";

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
        return favoriteArticles;
    }

    public int getFavoriteCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ? AND date_expiration > NOW()";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // Vérifier si un article est déjà dans les favoris d'un utilisateur
    public boolean isArticleInFavorites(Integer articleId, Integer userId) throws SQLException {
        String query = "SELECT 1 FROM favorie WHERE article_id = ? AND user_id = ? AND date_expiration > NOW() LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            statement.setInt(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    // Récupérer toutes les catégories d'articles
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
}
