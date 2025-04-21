package org.example.pidev.services;

import org.example.pidev.entities.Blog;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceBlog implements IService<Blog> {
    private final Connection cnx;

    public ServiceBlog() {
        cnx = MyDatabase.getInstance().getMyConnection();
        if (cnx == null) {
            System.out.println("Impossible d'établir une connexion à la base de données.");
        }
    }

    @Override
    public void ajouter(Blog blog) throws SQLException {
        String req = "INSERT INTO blog (title, content, publishedDate, image, likes, dislikes) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, blog.getTitle());
        ps.setString(2, blog.getContent());
        ps.setDate(3, blog.getPublishedDate());
        ps.setString(4, blog.getImage());
        ps.setInt(5, blog.getLikes());
        ps.setInt(6, blog.getDislikes());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Blog blog) throws SQLException {
        String req = "UPDATE blog SET title=?, content=?, publishedDate=?, image=?, likes=?, dislikes=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, blog.getTitle());
        ps.setString(2, blog.getContent());
        ps.setDate(3, blog.getPublishedDate());
        ps.setString(4, blog.getImage());
        ps.setInt(5, blog.getLikes());
        ps.setInt(6, blog.getDislikes());
        ps.setInt(7, blog.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM blog WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Blog> afficher() throws SQLException {
        List<Blog> blogs = new ArrayList<>();
        String req = "SELECT * FROM blog";  // Or use specific column names
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                blogs.add(new Blog(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getDate("publishedDate"), // Changed to match your actual column name
                        rs.getString("image"),
                        rs.getInt("likes"),
                        rs.getInt("dislikes")
                ));
            }
        }
        return blogs;
    }
    public Blog getBlogById(int id) throws SQLException {
        String req = "SELECT * FROM blog WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Blog(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getDate("publishedDate"),
                    rs.getString("image"),
                    rs.getInt("likes"),
                    rs.getInt("dislikes")
            );
        }
        throw new SQLException("Blog not found with ID: " + id);
    }




    public boolean voteBlog(int blogId, int userId, boolean isLike) throws SQLException {
        String voteType = isLike ? "LIKE" : "DISLIKE";

        // Check existing vote
        String checkQuery = "SELECT vote_type FROM blog_vote WHERE blog_id = ? AND user_id = ?";
        PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
        checkStmt.setInt(1, blogId);
        checkStmt.setInt(2, userId);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            String existingVote = rs.getString("vote_type");

            if (existingVote.equals(voteType)) {
                // Already voted same type → do nothing
                return false;
            }

            // Opposite vote → update vote and adjust counts
            String updateVote = "UPDATE blog_vote SET vote_type = ? WHERE blog_id = ? AND user_id = ?";
            PreparedStatement updateVoteStmt = cnx.prepareStatement(updateVote);
            updateVoteStmt.setString(1, voteType);
            updateVoteStmt.setInt(2, blogId);
            updateVoteStmt.setInt(3, userId);
            updateVoteStmt.executeUpdate();

            // Adjust blog counts
            String updateCounts = isLike ?
                    "UPDATE blog SET likes = likes + 1, dislikes = dislikes - 1 WHERE id = ?" :
                    "UPDATE blog SET dislikes = dislikes + 1, likes = likes - 1 WHERE id = ?";
            PreparedStatement updateCountsStmt = cnx.prepareStatement(updateCounts);
            updateCountsStmt.setInt(1, blogId);
            updateCountsStmt.executeUpdate();

            return true;
        }

        // No previous vote → insert
        String insertVote = "INSERT INTO blog_vote (blog_id, user_id, vote_type) VALUES (?, ?, ?)";
        PreparedStatement voteStmt = cnx.prepareStatement(insertVote);
        voteStmt.setInt(1, blogId);
        voteStmt.setInt(2, userId);
        voteStmt.setString(3, voteType);
        voteStmt.executeUpdate();

        String updateBlog = isLike ?
                "UPDATE blog SET likes = likes + 1 WHERE id=?" :
                "UPDATE blog SET dislikes = dislikes + 1 WHERE id=?";
        PreparedStatement updateStmt = cnx.prepareStatement(updateBlog);
        updateStmt.setInt(1, blogId);
        updateStmt.executeUpdate();

        return true;
    }


}