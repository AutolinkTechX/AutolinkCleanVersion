package org.example.pidev.entities;

import java.time.LocalDateTime;

public class Accord {

    private Integer id;
    private LocalDateTime dateCreation;
    private LocalDateTime dateReception;
    private Float quantity;
    private String output;
    private MaterielRecyclable materielRecyclable;
    private Entreprise entreprise;

    // Getter and Setter for id
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Getter and Setter for dateCreation
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    // Getter and Setter for dateReception
    public LocalDateTime getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDateTime dateReception) {
        this.dateReception = dateReception;
    }

    // Getter and Setter for quantity
    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    // Getter and Setter for output
    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    // Getter and Setter for materielRecyclable
    public MaterielRecyclable getMaterielRecyclable() {
        return materielRecyclable;
    }

    public void setMaterielRecyclable(MaterielRecyclable materielRecyclable) {
        this.materielRecyclable = materielRecyclable;
    }

    // Getter and Setter for entreprise
    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }
}

