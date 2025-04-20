package org.example.pidev.entities;

import java.time.LocalDateTime;

public class Favorie {

    private Integer id;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private User user;
    private Article article;

    // Constructor
    public Favorie() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    @Override
    public String toString() {
        return "Favorie{" +
                "id=" + id +
                ", dateCreation=" + dateCreation +
                ", dateExpiration=" + dateExpiration +
                ", user=" + user +
                ", article=" + article +
                '}';
    }
}
