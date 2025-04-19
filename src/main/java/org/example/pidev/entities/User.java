package org.example.pidev.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;


public class User {

    private int id;
    private int roleId;
    private String name;
    private String lastName;
    private int phone;
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private String image_path;
    private String verificationToken;
    private boolean isVerified;
    private String resetToken;
    private LocalDateTime resetTokenExpiresAt;
    private Address address;


    public User() {
    }

    public User(int roleId, String name, String lastName, int phone, String email, String password, boolean isVerified, String resetToken, LocalDateTime resetTokenExpiresAt, Address address, String image_path) {
        this.roleId = roleId;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.isVerified = isVerified;
        this.address = address;
        this.image_path = image_path;
    }

    public User(String name, String lastName, int phone, String email, String password) {
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }

    public User(int id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }


    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getPhone() {
        return phone;
    }

    public void setPhone(int phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getImage_path() {
        return image_path;
    }

    public void setImage_path(String image_path) {
        this.image_path = image_path;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone=" + phone +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", isVerified=" + isVerified +
                ", address='" + address.getCity() + ", " + address.getStreet() + ", " +
                address.getPostalCode() + ", " + address.getCountry() + '\'' +
                ", verificationToken='" + verificationToken + '\'' +
                ", resetToken='" + resetToken + '\'' +
                ", resetTokenExpiresAt=" + resetTokenExpiresAt +
                ", image_path='" + image_path + '\'' +
                '}';
    }
}
