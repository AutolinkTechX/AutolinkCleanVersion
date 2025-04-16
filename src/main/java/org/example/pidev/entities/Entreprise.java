package org.example.pidev.entities;

import java.time.LocalDateTime;

public class Entreprise {
    private int id;
    private int roleId;
    private String companyName;
    private String email;
    private String phone;
    private String taxCode;
    private LocalDateTime createdAt;
    private boolean supplier;
    private String password;
    private String field;
    private String imagePath;
    private Address address;

    public Entreprise() {
    }

    // Constructor with all fields
    public Entreprise(int roleId, String companyName, String email, String phone, String taxCode,
                      LocalDateTime createdAt, boolean supplier, String password, String field, String imagePath,
                      Address address) {
        this.roleId = roleId;
        this.companyName = companyName;
        this.email = email;
        this.phone = phone;
        this.taxCode = taxCode;
        this.createdAt = createdAt;
        this.supplier = supplier;
        this.password = password;
        this.field = field;
        this.imagePath = imagePath;
        this.address = address;
    }

    public Entreprise(String companyName) {
        this.companyName = companyName;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getSupplier() {
        return supplier;
    }

    public void setSupplier(boolean supplier) {
        this.supplier = supplier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    // Convenience methods for the view
    public String getFullName() {
        return companyName;
    }

    public String getRoleName() {
        return "Entreprise";
    }

    @Override
    public String toString() {
        return "Entreprise{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", companyName='" + companyName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", taxCode='" + taxCode + '\'' +
                ", createdAt=" + createdAt +
                ", supplier=" + supplier +
                ", field='" + field + '\'' +
                ", address=" + address +
                '}';
    }
}
