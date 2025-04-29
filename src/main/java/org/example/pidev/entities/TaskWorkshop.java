package org.example.pidev.entities;

import java.sql.Timestamp;

public class TaskWorkshop {

    private int id;
    private String nom;
    private String description;
    private Timestamp startsAt;
    private Timestamp endsAt;
    private String status;
    private int workshopId;

    // Constructeurs
    public TaskWorkshop() {
    }

    public TaskWorkshop(int id, String nom, String description, Timestamp startsAt,
                        Timestamp endsAt, String status, int workshopId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.workshopId = workshopId;
    }

    // Getters et Setters
    public int getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(int workshopId) {
        this.workshopId = workshopId;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Timestamp startsAt) {
        this.startsAt = startsAt;
    }

    public Timestamp getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Timestamp endsAt) {
        this.endsAt = endsAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Méthode toString()
    @Override
    public String toString() {
        return "TaskWorkshop{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", startsAt=" + startsAt +
                ", endsAt=" + endsAt +
                ", status='" + status + '\'' +
                '}';
    }
}
