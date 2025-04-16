package org.example.pidev.services;

import org.example.pidev.entities.Article;
import org.example.pidev.entities.List_article;
import org.example.pidev.entities.User;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ListeArticleService {
    private final Connection connection;
    private final ArticleService articleService;
    private static final Logger logger = Logger.getLogger(ArticleService.class.getName());

    public ListeArticleService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.articleService = new ArticleService();
    }




    // Méthodes pour le panier
    public void ajouterArticleAuPanier(List_article listarticle) throws SQLException {
        String query = "INSERT INTO list_article (quantite, prix_unitaire, article_id, user_id) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, listarticle.getQuantite());
            pst.setDouble(2, listarticle.getPrixUnitaire());
            pst.setInt(3, listarticle.getArticle().getId());
            pst.setInt(4, listarticle.getUser().getId());

            pst.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Erreur lors de l'ajout au panier: " + e.getMessage());
        }
    }

    public List_article findArticleInCart(int userId, int articleId) throws SQLException {
        String query = "SELECT * FROM list_article WHERE user_id = ? AND article_id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, articleId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    List_article item = new List_article();
                    item.setId(rs.getInt("id"));
                    item.setQuantite(rs.getInt("quantite"));
                    item.setPrixUnitaire(rs.getDouble("prix_unitaire"));

                    Article article = articleService.getById(articleId);
                    item.setArticle(article);

                    User user = new User();
                    user.setId(userId);
                    item.setUser(user);

                    return item;
                }
            }
        }
        return null;
    }

    public void updateArticleQuantity(List_article item) throws SQLException {
        String query = "UPDATE list_article SET quantite = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, item.getQuantite());
            pst.setInt(2, item.getId());
            pst.executeUpdate();
        }
    }
/*
    public int getCartItemCount(int userId) throws SQLException {
        String query = "SELECT COALESCE(SUM(quantite), 0) FROM list_article WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public List<List_article> getCartItems(int userId) throws SQLException {
        String query = "SELECT la.*, a.nom, a.prix, a.image FROM list_article la " +
                "JOIN article a ON la.article_id = a.id " +
                "WHERE la.user_id = ?";

        List<List_article> cartItems = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                List_article item = new List_article();
                item.setId(rs.getInt("id"));

                Article article = new Article();
                article.setId(rs.getInt("article_id"));
                article.setNom(rs.getString("nom"));
                article.setPrix(rs.getDouble("prix"));
                article.setImage(rs.getString("image"));
                item.setArticle(article);

                User user = new User();
                user.setId(userId);
                item.setUser(user);

                item.setQuantite(rs.getInt("quantite"));
                item.setPrixUnitaire(rs.getDouble("prix_unitaire"));

                cartItems.add(item);
            }
        }
        return cartItems;
    }

 */

    public void removeFromCart(int itemId) throws SQLException {
        String query = "DELETE FROM list_article WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, itemId);
            pst.executeUpdate();
        }
    }

    // Méthodes pour les favoris
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
/*
    public void ajouterArticleFavori(int userId, int articleId) throws SQLException {
        if (!isArticleInFavorites(userId, articleId)) {
            String query = "INSERT INTO favorie (user_id, article_id) VALUES (?, ?)";
            try (PreparedStatement pst = connection.prepareStatement(query)) {
                pst.setInt(1, userId);
                pst.setInt(2, articleId);
                pst.executeUpdate();
            }
        }
    }

 */

    public boolean ajouterArticleFavori(int userId, int articleId) throws SQLException {
        // Vérifier d'abord si l'article existe
        if (!articleExists(articleId)) {
            throw new SQLException("L'article n'existe pas");
        }

        // Vérifier si l'article est déjà en favoris
        if (isArticleInFavorites(userId, articleId)) {
            return false;
        }

        String query = "INSERT INTO favorie (user_id, article_id, date_creation, date_expiration) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, articleId);
            pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().plusMonths(1))); // Expiration dans 1 mois

            return pst.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            // Si l'article est déjà en favoris (au cas où la vérification précédente échoue)
            return false;
        }
    }

    private boolean articleExists(int articleId) throws SQLException {
        String query = "SELECT COUNT(*) FROM article WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, articleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void supprimerArticleFavori(int userId, int articleId) throws SQLException {
        String query = "DELETE FROM favorie WHERE user_id = ? AND article_id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, articleId);
            pst.executeUpdate();
        }
    }
/*
    public int getFavoriteCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

 */
    public boolean removeFromFavorites(int userId, int articleId) throws SQLException {
        String query = "DELETE FROM favorie WHERE user_id = ? AND article_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, articleId);
            return statement.executeUpdate() > 0;
        }
    }
    public List<Article> getUserFavorites(int userId) throws SQLException {
        String query = "SELECT a.* FROM article a JOIN favorie f ON a.id = f.article_id WHERE f.user_id = ?";
        List<Article> favorites = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    favorites.add(mapResultSetToArticle(resultSet));
                }
            }
        }
        return favorites;
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
    public int getFavoriteCount(int userId) throws SQLException {
        String query = "SELECT COUNT(*) FROM favorie WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    public int getCartItemCount(int userId) throws SQLException {
        String query = "SELECT COALESCE(SUM(quantite), 0) FROM list_article WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public List<List_article> getCartItems(int userId) throws SQLException {
        String query = "SELECT la.*, a.nom, a.prix, a.image FROM list_article la " +
                "JOIN article a ON la.article_id = a.id " +
                "WHERE la.user_id = ?";

        List<List_article> cartItems = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                List_article item = new List_article();
                item.setId(rs.getInt("id"));

                Article article = new Article();
                article.setId(rs.getInt("article_id"));
                article.setNom(rs.getString("nom"));
                article.setPrix(rs.getDouble("prix"));
                article.setImage(rs.getString("image"));
                item.setArticle(article);

                User user = new User();
                user.setId(userId);
                item.setUser(user);

                item.setQuantite(rs.getInt("quantite"));
                item.setPrixUnitaire(rs.getDouble("prix_unitaire"));

                cartItems.add(item);
            }
        }
        return cartItems;
    }
}