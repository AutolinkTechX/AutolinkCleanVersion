package org.example.pidev.services;

import org.example.pidev.entities.Entreprise;
import org.example.pidev.utils.MyDatabase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntrepriseService implements IService<Entreprise> {

    private final Connection connection;

    public EntrepriseService() {
        connection = MyDatabase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(Entreprise entreprise) throws SQLException {
        String query = "INSERT INTO entreprise (role_id, company_name, email, phone, tax_code, created_at, supplier, password, field, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, 3); // role_id for entreprise
            ps.setString(2, entreprise.getCompanyName());
            ps.setString(3, entreprise.getEmail());
            ps.setString(4, entreprise.getPhone());
            ps.setString(5, entreprise.getTaxCode());
            ps.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setBoolean(7, entreprise.getSupplier());
            ps.setString(8, entreprise.getPassword());
            ps.setString(9, entreprise.getField());
            ps.setString(10, entreprise.getImagePath());
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                entreprise.setId(rs.getInt(1));
            }
        }
    }

    @Override
    public void modifier(Entreprise entreprise) throws SQLException {
        String query = "UPDATE entreprise SET company_name=?, email=?, phone=?, tax_code=?, supplier=?, field=?, image_path=? WHERE id=?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entreprise.getCompanyName());
            ps.setString(2, entreprise.getEmail());
            ps.setString(3, entreprise.getPhone());
            ps.setString(4, entreprise.getTaxCode());
            ps.setBoolean(5, entreprise.getSupplier());
            ps.setString(6, entreprise.getField());
            ps.setString(7, entreprise.getImagePath());
            ps.setInt(8, entreprise.getId());
            
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String query = "DELETE FROM entreprise WHERE id=?";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Entreprise> afficher() throws SQLException {
        List<Entreprise> entreprises = new ArrayList<>();
        String query = "SELECT * FROM entreprise WHERE role_id = 3"; // Assuming role_id 3 is for entreprises
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                Entreprise entreprise = new Entreprise();
                entreprise.setId(rs.getInt("id"));
                entreprise.setRoleId(rs.getInt("role_id"));
                entreprise.setCompanyName(rs.getString("company_name"));
                entreprise.setEmail(rs.getString("email"));
                entreprise.setPhone(rs.getString("phone"));
                entreprise.setTaxCode(rs.getString("tax_code"));
                entreprise.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                entreprise.setSupplier(rs.getBoolean("supplier"));
                entreprise.setPassword(rs.getString("password"));
                entreprise.setField(rs.getString("field"));
                entreprise.setImagePath(rs.getString("image_path"));
                
                entreprises.add(entreprise);
            }
        }
        
        return entreprises;
    }

    public Entreprise authenticate(String email, String password) throws SQLException {
        String query = "SELECT e.*, r.name as role_name FROM entreprise e " +
                      "JOIN role r ON e.role_id = r.id " +
                      "WHERE e.email = ? AND e.supplier = 1";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                // Convert $2y$ to $2a$ format for jBCrypt compatibility
                if (hashedPassword.startsWith("$2y$")) {
                    hashedPassword = "$2a$" + hashedPassword.substring(4);
                }
                // Verify the password using BCrypt
                if (BCrypt.checkpw(password, hashedPassword)) {
                    Entreprise entreprise = new Entreprise();
                    entreprise.setId(rs.getInt("id"));
                    entreprise.setEmail(rs.getString("email"));
                    entreprise.setPassword(rs.getString("password"));
                    entreprise.setRoleId(rs.getInt("role_id"));
                    entreprise.setCompanyName(rs.getString("company_name"));
                    entreprise.setSupplier(rs.getBoolean("supplier"));
                    return entreprise;
                }
            }
            throw new IllegalArgumentException("Email or password incorrect, or account is not a supplier");
        }
    }

    public String getRoleName(int roleId) throws SQLException {
        String query = "SELECT name FROM role WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, roleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
            throw new IllegalArgumentException("Role not found");
        }
    }

    public Entreprise getById(int id) throws SQLException {
        String query = "SELECT * FROM entreprise WHERE id = ? AND role_id = 3";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Entreprise entreprise = new Entreprise();
                    entreprise.setId(rs.getInt("id"));
                    entreprise.setRoleId(rs.getInt("role_id"));
                    entreprise.setCompanyName(rs.getString("company_name"));
                    entreprise.setEmail(rs.getString("email"));
                    entreprise.setPhone(rs.getString("phone"));
                    entreprise.setTaxCode(rs.getString("tax_code"));
                    entreprise.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    entreprise.setSupplier(rs.getBoolean("supplier"));
                    entreprise.setPassword(rs.getString("password"));
                    entreprise.setField(rs.getString("field"));
                    entreprise.setImagePath(rs.getString("image_path"));
                    
                    return entreprise;
                }
            }
        }
        
        return null;
    }

    public List<Entreprise> getAllEntreprises() throws SQLException {
        System.out.println("Starting getAllEntreprises...");
        List<Entreprise> entreprises = new ArrayList<>();
        String query = "SELECT * FROM entreprise WHERE role_id = 3"; // Assuming role_id 3 is for entreprises

        Connection conn = MyDatabase.getInstance().getMyConnection();
        if (conn == null) {
            System.out.println("Database connection is null");
            throw new SQLException("Database connection is not available");
        }
        System.out.println("Database connection obtained successfully");

        try (PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            
            System.out.println("Executing query: " + query);
            int count = 0;
            
            while (rs.next()) {
                count++;
                System.out.println("Processing entreprise #" + count);
                Entreprise entreprise = new Entreprise();
                entreprise.setId(rs.getInt("id"));
                entreprise.setCompanyName(rs.getString("company_name"));
                entreprise.setEmail(rs.getString("email"));
                entreprise.setPhone(rs.getString("phone"));
                entreprise.setImagePath(rs.getString("image_path"));
                
                System.out.println("Loaded entreprise: " + entreprise.getCompanyName() + 
                                 " (ID: " + entreprise.getId() + 
                                 ", Email: " + entreprise.getEmail() + 
                                 ", Phone: " + entreprise.getPhone() + ")");
                
                entreprises.add(entreprise);
            }
            System.out.println("Total entreprises found: " + count);
        } catch (SQLException e) {
            System.out.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return entreprises;
    }
} 