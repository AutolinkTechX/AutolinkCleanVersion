package org.example.pidev.entities;

import java.sql.Date;
import java.util.Objects;

public class Blog {

    private int id;
    private String title;
    private String content;
    private Date publishedDate;
    private String image;
    private int likes;
    private int dislikes;




    // Default constructor
    public Blog() {}

    // Constructor for the basic fields
    public Blog(String title, String content, Date publishedDate) {
        this.title = title;
        this.content = content;
        this.publishedDate = publishedDate;
        this.image = ""; // Default empty string for image
        this.likes = 0;  // Default 0 likes
        this.dislikes = 0; // Default 0 dislikes
    }

    // Constructor with all fields (used for full data creation)
    public Blog(int id, String title, String content, Date publishedDate, String image, int likes, int dislikes) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.publishedDate = publishedDate;
        this.image = image;
        this.likes = likes;
        this.dislikes = dislikes;
    }

    // Getters and Setters for all fields
    public int getId() {
        return id;
    }

    public String getImage() {
        return image;
    }

    public void setId(int id) {
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

    public Date getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Date publishedDate) {
        this.publishedDate = publishedDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Blog)) return false;
        Blog blog = (Blog) o;
        return id == blog.id &&
                likes == blog.likes &&
                dislikes == blog.dislikes &&
                Objects.equals(title, blog.title) &&
                Objects.equals(content, blog.content) &&
                Objects.equals(publishedDate, blog.publishedDate) &&
                Objects.equals(image, blog.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, publishedDate, image, likes, dislikes);
    }

    @Override
    public String toString() {
        return "Blog{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", publishedDate=" + publishedDate +
                ", image='" + image + '\'' +
                ", likes=" + likes +
                ", dislikes=" + dislikes +
                '}';
    }
}
