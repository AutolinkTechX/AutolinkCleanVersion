package org.example.pidev.services;

import org.example.pidev.entities.User;
import org.example.pidev.utils.MyDatabase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(User user) throws SQLException {
        String sql = "INSERT INTO `user`(`name`, `last_name`, `phone`, `email`, `password`, `role_id`, `created_at`, `is_verified`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLastName());
            stmt.setInt(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            // Encrypt the password using BCrypt
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            stmt.setString(5, hashedPassword);
            stmt.setInt(6, user.getRoleId());
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            stmt.setBoolean(8, true); // Set is_verified to true for new admins

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;  // rethrow the exception or handle it appropriately
        }
    }

    @Override
    public void modifier(User user) throws SQLException {
        // Check if password is being updated
        String sql;
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            sql = "UPDATE `user` SET `name` = ?, `last_name` = ?, `phone` = ?, `email` = ?, `password` = ? WHERE `id` = ?";
        } else {
            sql = "UPDATE `user` SET `name` = ?, `last_name` = ?, `phone` = ?, `email` = ? WHERE `id` = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLastName());
            stmt.setInt(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                // Encrypt the password using BCrypt
                String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
                stmt.setString(5, hashedPassword);
                stmt.setInt(6, user.getId());
            } else {
                stmt.setInt(5, user.getId());
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM `user` WHERE `id` = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);  // Set the ID of the user to delete

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("User deleted successfully.");
            } else {
                System.out.println("No user found with the given ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;  // Rethrow the exception or handle it appropriately
        }
    }

    @Override
    public List<User> afficher() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT `id`, `name`, `last_name`, `phone`, `email`, `password` FROM `user`";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                User user = new User(
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getInt("phone"),
                        rs.getString("email"),
                        rs.getString("password")
                );
                user.setId(rs.getInt("id"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;  // Rethrow the exception or handle it as needed
        }

        return users;
    }

    public User authenticate(String email, String password) throws SQLException {
        String query = "SELECT u.*, r.name as role_name FROM user u " +
                      "JOIN role r ON u.role_id = r.id " +
                      "WHERE u.email = ?";
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
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setRoleId(rs.getInt("role_id"));
                    user.setPhone(rs.getInt("phone"));
                    user.setImage_path(rs.getString("image_path"));
                    return user;
                }
            }
            throw new IllegalArgumentException("Email or password incorrect");
        }
    }

    public void sendPasswordReset(String email) throws SQLException {
        // Implement password reset logic
        System.out.println("Password reset requested for: " + email);
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

    public List<User> getAllAdmins() throws SQLException {
        System.out.println("Getting all admins from database...");
        List<User> admins = new ArrayList<>();
        
        // First check if we have a valid connection
        if (connection == null || connection.isClosed()) {
            System.out.println("Database connection is null or closed!");
            throw new SQLException("Database connection is not available");
        }
        
        // Try a simpler query first to test the connection
        String query = "SELECT * FROM user WHERE role_id = 1"; // Assuming role_id 1 is for admins
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            System.out.println("Executing query: " + query);
            
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Query executed successfully");
            
            while (resultSet.next()) {
                User admin = new User();
                admin.setId(resultSet.getInt("id"));
                admin.setName(resultSet.getString("name"));
                admin.setLastName(resultSet.getString("last_name"));
                admin.setPhone(resultSet.getInt("phone"));
                admin.setEmail(resultSet.getString("email"));
                admin.setRoleId(resultSet.getInt("role_id"));
                
                System.out.println("Found admin: " + admin.getName() + " " + admin.getLastName() + 
                                 " (ID: " + admin.getId() + ", Email: " + admin.getEmail() + ")");
                
                admins.add(admin);
            }
            
            System.out.println("Total admins found: " + admins.size());
            
            if (admins.isEmpty()) {
                System.out.println("No admins found in the database!");
            }
            
        } catch (SQLException e) {
            System.out.println("Error in getAllAdmins: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw e;
        }
        
        return admins;
    }

    public List<User> getAllClients() throws SQLException {
        System.out.println("Getting all clients from database...");
        List<User> clients = new ArrayList<>();
        
        // First check if we have a valid connection
        if (connection == null || connection.isClosed()) {
            System.out.println("Database connection is null or closed!");
            throw new SQLException("Database connection is not available");
        }
        
        // Try a simpler query first to test the connection
        String query = "SELECT * FROM user WHERE role_id = 2"; // Assuming role_id 2 is for clients
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            System.out.println("Executing query: " + query);
            
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Query executed successfully");
            
            while (resultSet.next()) {
                User client = new User();
                client.setId(resultSet.getInt("id"));
                client.setName(resultSet.getString("name"));
                client.setLastName(resultSet.getString("last_name"));
                client.setPhone(resultSet.getInt("phone"));
                client.setEmail(resultSet.getString("email"));
                client.setRoleId(resultSet.getInt("role_id"));
                client.setImage_path(resultSet.getString("image_path"));
                
                System.out.println("Found client: " + client.getName() + " " + client.getLastName() + 
                                 " (ID: " + client.getId() + ", Email: " + client.getEmail() + ")");
                
                clients.add(client);
            }
            
            System.out.println("Total clients found: " + clients.size());
            
            if (clients.isEmpty()) {
                System.out.println("No clients found in the database!");
            }
            
        } catch (SQLException e) {
            System.out.println("Error in getAllClients: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw e;
        }
        
        return clients;
    }




    public User getUserById(int id) throws SQLException {
        String req = "SELECT * FROM user WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) { // Remplacé cnx par connection
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setLastName(rs.getString("last_name")); // Attention au nom de colonne
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getInt("phone"));
                    // Tu peux compléter si tu veux set d'autres champs (image, createdAt, etc.)
                    return user;
                }
            }
        }
        return null;
    }
}
