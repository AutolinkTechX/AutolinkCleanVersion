package org.example.pidev.entities;

import java.sql.Date;
import java.util.Objects;

public class Comment {

    private int id;
    private Blog blog;
    private String content;
    private Date created_at;
    private User user;

    // Constructeurs
    public Comment() {}

    public Comment(Blog blog, String content, Date created_at, User user) {
        this.blog = blog;
        this.content = content;
        this.created_at = created_at;
        this.user = user;
    }

    public Comment(int id, Blog blog, String content, Date created_at, User user) {
        this.id = id;
        this.blog = blog;
        this.content = content;
        this.created_at = created_at;
        this.user = user;
    }

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return id == comment.id &&
                Objects.equals(blog, comment.blog) &&
                Objects.equals(content, comment.content) &&
                Objects.equals(created_at, comment.created_at) &&
                Objects.equals(user, comment.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, blog, content, created_at, user);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", blog=" + blog +
                ", content='" + content + '\'' +
                ", created_at=" + created_at +
                ", user=" + user +
                '}';
    }
}
