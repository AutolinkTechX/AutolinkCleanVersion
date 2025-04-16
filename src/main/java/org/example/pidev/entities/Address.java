package org.example.pidev.entities;

public class Address {
    private String street;
    private String city;
    private String postalCode;
    private String country;

    // Constructor
    public Address() {}
    public Address(String street, String city, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    // Getters
    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    // Setters
    public void setStreet(String street) {
        this.street = street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return street + ", " + city + ", " + postalCode + ", " + country;
    }
}
