package org.example.pidev.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

@Entity
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime dateCommande;

    private String modePaiement;

    private Double total;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @Column(name = "article_ids", columnDefinition = "LONGTEXT")
    private String articleIds; // Stocker les ids des articles sous forme de String (JSON)

    @Column(name = "quantites", columnDefinition = "LONGTEXT")
    private String quantites; // Stocker les quantités sous forme de String (JSON)

    // Déclaration de l'ObjectMapper
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructor
    public Commande() {
        // Initialisation des listes (seront converties en String lors de la persistance)
        this.articleIds = "";
        this.quantites = "";
    }

    public LocalDateTime getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDateTime dateCommande) {
        this.dateCommande = dateCommande;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }

    public String getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(String articleIds) {
        this.articleIds = articleIds;
    }

    public String getQuantites() {
        return quantites;
    }

    public void setQuantites(String quantites) {
        this.quantites = quantites;
    }

    // Récupérer les quantités sous forme de Map
    public Map<Integer, Integer> getQuantitesMap() {
        try {
            return objectMapper.readValue(quantites, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la conversion des quantités en Map : " + e.getMessage(), e);
        }
    }
}
