package org.example.pidev.entities;

import javafx.beans.property.*;

import java.sql.Timestamp;

public class Workshop {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<Timestamp> startsAt = new SimpleObjectProperty<>();
    private final ObjectProperty<Timestamp> endsAt = new SimpleObjectProperty<>();
    private final StringProperty location = new SimpleStringProperty();
    private final StringProperty image = new SimpleStringProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final IntegerProperty availablePlaces = new SimpleIntegerProperty(0);
    private String videoPath;
    private final DoubleProperty rating = new SimpleDoubleProperty();
    private final IntegerProperty user_id = new SimpleIntegerProperty(); // New user_id attribute

    // Constructeurs
    public Workshop() {
    }

    public Workshop(int id, String name, String description, Timestamp startsAt, Timestamp endsAt,
                    String location, String image, double price, int availablePlaces, int user_id) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
        this.startsAt.set(startsAt);
        this.endsAt.set(endsAt);
        this.location.set(location);
        this.image.set(image);
        this.price.set(price);
        this.availablePlaces.set(availablePlaces);
        this.user_id.set(user_id);
    }

    // Getters et Setters standards
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public Timestamp getStartsAt() {
        return startsAt.get();
    }

    public void setStartsAt(Timestamp startsAt) {
        this.startsAt.set(startsAt);
    }

    public Timestamp getEndsAt() {
        return endsAt.get();
    }

    public void setEndsAt(Timestamp endsAt) {
        this.endsAt.set(endsAt);
    }

    public String getLocation() {
        return location.get();
    }

    public void setLocation(String location) {
        this.location.set(location);
    }

    public double getRating() {
        return rating.get();
    }

    public void setRating(double rating) {
        this.rating.set(rating);
    }

    public DoubleProperty ratingProperty() {
        return rating;
    }

    public String getImage() {
        return image.get();
    }

    public void setImage(String image) {
        this.image.set(image);
    }

    public double getPrice() {
        return price.get();
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    public int getAvailablePlaces() {
        return availablePlaces.get();
    }

    public void setAvailablePlaces(int availablePlaces) {
        this.availablePlaces.set(availablePlaces);
    }

    public int getUser_id() {
        return user_id.get();
    }

    public void setUser_id(int user_id) {
        this.user_id.set(user_id);
    }

    // Property getters
    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObjectProperty<Timestamp> startsAtProperty() {
        return startsAt;
    }

    public ObjectProperty<Timestamp> endsAtProperty() {
        return endsAt;
    }

    public StringProperty locationProperty() {
        return location;
    }

    public StringProperty imageProperty() {
        return image;
    }

    public DoubleProperty priceProperty() {
        return price;
    }

    public IntegerProperty availablePlacesProperty() {
        return availablePlaces;
    }

    public IntegerProperty user_idProperty() {
        return user_id;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    @Override
    public String toString() {
        return getName();
    }
}