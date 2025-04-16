package org.example.pidev.entities;

import java.util.ArrayList;
import java.util.List;

public class Blog {

    private Long id;
    private String title;
    private String content;
    private java.time.LocalDate publishedDate;
    private List<Comment> comments = new ArrayList<>();
    private String image;
    private int likes = 0;
    private int dislikes = 0;

    // Constructor
    public Blog() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public java.time.LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(java.time.LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
    }
}
