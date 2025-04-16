package org.example.pidev.services;

import org.example.pidev.entities.Article;
import org.example.pidev.entities.List_article;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ArticleService {

    private static final Logger logger = Logger.getLogger(ArticleService.class.getName());
    private final Connection connection;

    public ArticleService() {
        this.connection = MyDatabase.getInstance().getMyConnection();
    }

    public void create(Article article) throws SQLException {
        String query = "INSERT INTO article (nom, description, category, image, prix, quantitestock, datecreation) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, article.getNom());
            statement.setString(2, article.getDescription());
            statement.setString(3, article.getCategory());
            statement.setString(4, article.getImage());
            statement.setDouble(5, article.getPrix());
            statement.setInt(6, article.getQuantitestock());
            statement.setTimestamp(7, Timestamp.valueOf(article.getDatecreation()));

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating article failed, no rows affected.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    article.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating article failed, no ID obtained.");
                }
            }
        }
    }

    public void update(Article article) throws SQLException {
        String query = "UPDATE article SET nom = ?, description = ?, category = ?, image = ?, " +
                "prix = ?, quantitestock = ?, datecreation = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, article.getNom());
            statement.setString(2, article.getDescription());
            statement.setString(3, article.getCategory());
            statement.setString(4, article.getImage());
            statement.setDouble(5, article.getPrix());
            statement.setInt(6, article.getQuantitestock());
            statement.setTimestamp(7, Timestamp.valueOf(article.getDatecreation()));
            statement.setInt(8, article.getId());

            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String query = "DELETE FROM article WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<String> getAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT category FROM article WHERE quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                categories.add(resultSet.getString("category"));
            }
        }
        return categories;
    }

    public List<Article> getAllArticle() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM article";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                articles.add(mapResultSetToArticle(resultSet));
            }
        }
        return articles;
    }

    public List<Article> getAllArticles() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM article WHERE quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                articles.add(mapResultSetToArticle(resultSet));
            }
        }
        return articles;
    }

    public List<Article> getAllArticlesIncludingZeroStock() throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM article";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                articles.add(mapResultSetToArticle(resultSet));
            }
        }
        return articles;
    }

    public List<Article> searchArticles(String searchTerm) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM article WHERE (nom LIKE ? OR category LIKE ?) AND quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "%" + searchTerm + "%");
            statement.setString(2, "%" + searchTerm + "%");

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    articles.add(mapResultSetToArticle(resultSet));
                }
            }
        }
        return articles;
    }

    /*
    public List<Article> getByCategory(String category) throws SQLException {
        List<Article> articles = new ArrayList<>();
        String query = "SELECT * FROM article WHERE category = ? AND quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    articles.add(mapResultSetToArticle(resultSet));
                }
            }
        }
        return articles;
    }
*/
    public List<Article> getByCategory(String category) throws SQLException {
        String query = "SELECT * FROM article WHERE category = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, category);
            ResultSet resultSet = statement.executeQuery();
            List<Article> articles = new ArrayList<>();
            while (resultSet.next()) {
                // Map result set to Article objects
                articles.add(mapResultSetToArticle(resultSet));
            }
            return articles;
        }
    }

    public Article getById(int id) throws SQLException {
        String query = "SELECT * FROM article WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToArticle(resultSet);
                }
            }
        }
        return null;
    }

    public boolean ajouterArticleFavori(int userId, int articleId) throws SQLException {
        String query = "INSERT INTO favorie (user_id, article_id, date_creation, date_expiration) VALUES (?, ?, ?, ?)";

        LocalDateTime expirationDate = LocalDateTime.now().plusDays(30);

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, articleId);
            pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pst.setTimestamp(4, Timestamp.valueOf(expirationDate));

            try {
                return pst.executeUpdate() > 0;
            } catch (SQLIntegrityConstraintViolationException e) {
                // Si déjà en favoris
                return false;
            }
        }
    }

    public boolean isArticleInFavorites(int userId, int articleId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ? AND article_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, articleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void ajouterArticleAuPanier(List_article listarticle) throws SQLException {
        // Vérifier d'abord que l'article est en stock
        Article article = getById(listarticle.getArticle().getId());
        if (article == null || article.getQuantitestock() < listarticle.getQuantite()) {
            throw new SQLException("Quantité demandée non disponible en stock");
        }

        String query = "INSERT INTO list_article (quantite, prix_unitaire, article_id, user_id) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, listarticle.getQuantite());
            statement.setDouble(2, listarticle.getPrixUnitaire());
            statement.setInt(3, listarticle.getArticle().getId());
            statement.setInt(4, listarticle.getUser().getId());
            statement.executeUpdate();
        }
    }

    private Article mapResultSetToArticle(ResultSet resultSet) throws SQLException {
        Article article = new Article();
        article.setId(resultSet.getInt("id"));
        article.setNom(resultSet.getString("nom"));
        article.setDescription(resultSet.getString("description"));
        article.setCategory(resultSet.getString("category"));
        article.setImage(resultSet.getString("image"));
        article.setPrix(resultSet.getDouble("prix"));
        article.setQuantitestock(resultSet.getInt("quantitestock"));

        Timestamp timestamp = resultSet.getTimestamp("datecreation");
        if (timestamp != null) {
            article.setDatecreation(timestamp.toLocalDateTime());
        }
        return article;
    }

    public List<Article> getUserFavorites(int userId) throws SQLException {
        String query = "SELECT a.* FROM article a JOIN favorie f ON a.id = f.article_id WHERE f.user_id = ?";
        List<Article> favorites = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Article article = new Article();
                article.setId(rs.getInt("id"));
                article.setNom(rs.getString("nom"));
                article.setPrix(rs.getDouble("prix"));
                article.setImage(rs.getString("image"));
                // Ajoutez d'autres propriétés si nécessaire
                favorites.add(article);
            }
        }
        return favorites;
    }

    public int getFavoriteCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void removeFromFavorites(int userId, int articleId) throws SQLException {
        String query = "DELETE FROM favorie WHERE user_id = ? AND article_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, articleId);
            statement.executeUpdate();
        }
    }


    public Article getArticleById(int id) throws SQLException {
        String query = "SELECT * FROM article WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Article article = new Article();
                article.setId(resultSet.getInt("id"));
                article.setNom(resultSet.getString("nom"));
                article.setPrix(resultSet.getDouble("prix"));
                // Ajoutez d'autres champs si nécessaire
                return article;
            }
        }
        return null;
    }

    // Méthode pour mettre à jour le stock après commande
    public boolean mettreAJourStock(int articleId, int quantiteVendue) throws SQLException {
        String query = "UPDATE article SET quantitestock = quantitestock - ? WHERE id = ? AND quantitestock >= ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, quantiteVendue);
            stmt.setInt(2, articleId);
            stmt.setInt(3, quantiteVendue);

            return stmt.executeUpdate() > 0;
        }
    }

    // Méthode pour récupérer le prix d'un article
    public double getPrixArticle(int articleId) throws SQLException {
        String query = "SELECT prix FROM article WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("prix");
            }
        }
        throw new SQLException("Article non trouvé");
    }

    // Méthode pour vérifier la disponibilité d'un article
    public boolean verifierDisponibilite(int articleId, int quantite) throws SQLException {
        String query = "SELECT quantitestock FROM article WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, articleId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantitestock") >= quantite;
            }
        }
        return false;
    }

    public boolean productNameExists(String productName) throws SQLException {
        String query = "SELECT COUNT(*) FROM article WHERE LOWER(nom) = LOWER(?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, productName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public Set<String> getAllCategoriesFromArticles() throws SQLException {
        Set<String> categories = new HashSet<>();
        String query = "SELECT DISTINCT category FROM article WHERE quantitestock > 0";

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                categories.add(resultSet.getString("category"));
            }
        }
        return categories;
    }
}