package org.example.pidev.entities;

import java.time.LocalDateTime;

public class Facture {

    private Integer id;
    private Double montant;
    private LocalDateTime datetime;
    private Commande commande;
    private User client;

    // Constructor
    public Facture() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    public Commande getCommande() {
        return commande;
    }

    @Override
    public String toString() {
        return "Facture{" +
                "id=" + id +
                ", montant=" + montant +
                ", datetime=" + datetime +
                ", commande=" + commande +
                ", client=" + client +
                '}';
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }
}
