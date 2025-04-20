package org.example.pidev.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Article {

    private Integer id;
    private String description;
    private String nom;
    private String category;
    private String image;
    private LocalDateTime datecreation;
    private Double prix;
    private Integer quantitestock;
    private List<List_article> listarticles;
    private List<Favorie> favories;

    // Constructor
    public Article() {
        this.listarticles = new ArrayList<>();
        this.favories = new ArrayList<>();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDateTime getDatecreation() {
        return datecreation;
    }

    public void setDatecreation(LocalDateTime datecreation) {
        this.datecreation = datecreation;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public Integer getQuantitestock() {
        return quantitestock;
    }

    public void setQuantitestock(Integer quantitestock) {
        this.quantitestock = quantitestock;
    }

    public List<List_article> getListArticles() {
        return listarticles;
    }

    public void setListArticles(List<List_article> listarticles) {
        this.listarticles = listarticles;
    }

    public List<Favorie> getFavories() {
        return favories;
    }

    public void setFavories(List<Favorie> favories) {
        this.favories = favories;
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", nom='" + nom + '\'' +
                ", category='" + category + '\'' +
                ", image='" + image + '\'' +
                ", datecreation=" + datecreation +
                ", prix=" + prix +
                ", quantitestock=" + quantitestock +
                ", listarticles=" + listarticles +
                ", favories=" + favories +
                '}';
    }
}