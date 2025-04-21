package org.example.pidev.services;

import org.example.pidev.entities.Blog;
import org.example.pidev.entities.Comment;
import org.example.pidev.entities.User;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceComment implements IService<Comment> {
    private final Connection cnx;
    private final ServiceBlog serviceBlog;
    private final UserService serviceUser;

    public ServiceComment() {
        this.cnx = MyDatabase.getInstance().getMyConnection();
        this.serviceBlog = new ServiceBlog();
        this.serviceUser = new UserService();
    }

    @Override
    public void ajouter(Comment comment) throws SQLException {
        String req = "INSERT INTO comment (blog_id, content, created_at, id_user) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, comment.getBlog().getId());
            ps.setString(2, comment.getContent());
            ps.setDate(3, comment.getCreated_at());
            ps.setInt(4, comment.getUser().getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Comment comment) throws SQLException {
        String req = "UPDATE comment SET blog_id=?, content=?, created_at=?, id_user=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, comment.getBlog().getId());
            ps.setString(2, comment.getContent());
            ps.setDate(3, comment.getCreated_at());
            ps.setInt(4, comment.getUser().getId());
            ps.setInt(5, comment.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM comment WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Comment> afficher() throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String req = "SELECT * FROM comment";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {

            while (rs.next()) {
                Blog blog = serviceBlog.getBlogById(rs.getInt("blog_id"));
                User user = serviceUser.getUserById(rs.getInt("id_user"));

                Comment comment = new Comment(
                        rs.getInt("id"),
                        blog,
                        rs.getString("content"),
                        rs.getDate("created_at"),
                        user
                );
                comments.add(comment);
            }
        }
        return comments;
    }

    public List<Comment> getCommentsByBlog(int blogId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String req = "SELECT * FROM comment WHERE blog_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, blogId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Blog blog = serviceBlog.getBlogById(blogId);
                    User user = serviceUser.getUserById(rs.getInt("id_user"));

                    Comment comment = new Comment(
                            rs.getInt("id"),
                            blog,
                            rs.getString("content"),
                            rs.getDate("created_at"),
                            user
                    );
                    comments.add(comment);
                }
            }
        }
        return comments;
    }

    public List<Comment> getCommentsByUser(int userId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String req = "SELECT * FROM comment WHERE id_user=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Blog blog = serviceBlog.getBlogById(rs.getInt("blog_id"));
                    User user = serviceUser.getUserById(userId);

                    Comment comment = new Comment(
                            rs.getInt("id"),
                            blog,
                            rs.getString("content"),
                            rs.getDate("created_at"),
                            user
                    );
                    comments.add(comment);
                }
            }
        }
        return comments;
    }
}
