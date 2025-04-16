package org.example.pidev.entities;

import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.Enum.Type_materiel;

import java.time.LocalDateTime;

public class MaterielRecyclable {

    private Integer id;
    private String name;
    private String description;
    private LocalDateTime dateCreation;
    private Type_materiel type_materiel;
    private String image;
    private StatutEnum statut;
    private Entreprise entreprise;
    private User user;



    // Constructeur paramétré
    public MaterielRecyclable(String name, String description, LocalDateTime dateCreation,
                              Type_materiel type_materiel, String image, StatutEnum statut,
                              Entreprise entreprise) {
        this.name = name;
        this.description = description;
        this.dateCreation = dateCreation;
        this.type_materiel = type_materiel;
        this.image = image;
        this.statut = StatutEnum.EN_ATTENTE;
        this.entreprise = entreprise;
       /* this.user = user;*/
    }

    public MaterielRecyclable(){}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Type_materiel getType_materiel() {
        return type_materiel;
    }

    public void setType_materiel(String typeMateriel) {
        this.type_materiel = type_materiel;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public StatutEnum getStatut() {
        return statut;
    }

    public void setStatut(StatutEnum statut) {
        this.statut = statut;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

   /* public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }*/






    @Override
    public String toString() {
        return "MaterielRecyclable{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                ", Type_materiel='" + type_materiel + '\'' +
                ", image='" + image + '\'' +
                ", statut=" + statut +
                ", entreprise=" + entreprise +
                ", user=" + user +
                '}';
    }

}

