package org.example.pidev.services;

import org.example.pidev.entities.User;
import org.example.pidev.utils.EmailUtil;
import org.example.pidev.utils.MyDatabase;
import org.mindrot.jbcrypt.BCrypt;



import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Session;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import java.util.UUID;
import java.time.LocalDateTime;



public class UserService implements IService<User> {

    private final Connection connection;
    private final String smtpHostServer = "smtp.gmail.com";
    private final String AutolinkEmail = "flappywharf@gmail.com";
    private final String AutolinkPassword = "mctb jqmv quhy bpme";
    

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
                      "WHERE u.email = ? AND u.is_verified = true";
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
            if (emailExists(email) && !isUserVerified(email)) {
                System.out.println(isUserVerified(email));
                throw new IllegalArgumentException("Account not verified. Please check your email for verification link.");
            } else {
                System.out.println(isUserVerified(email));
                throw new IllegalArgumentException("Email or password incorrect");
            }
        }
    }

    public boolean isUserVerified(String email) throws SQLException {
        String query = "SELECT is_verified FROM user WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("is_verified");
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    public void sendAccountVerification(String email) throws SQLException {
        String verificationToken = UUID.randomUUID().toString();

        String query = "UPDATE user SET verification_token = ? WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, verificationToken);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }

	    
	    Properties props = System.getProperties();

	    props.put("mail.smtp.host", smtpHostServer);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(AutolinkEmail, AutolinkPassword);
			}
		};
		Session session = Session.getInstance(props, auth);

        String appLink = "Autolink://verify-account?token=" + verificationToken;

        String htmlContent = "Click the link below to verify your account: " + appLink + "\n";
		
		EmailUtil.sendEmail(session, email,"Account verification", htmlContent);
    }

    public boolean verifyAccountToken(String token) throws SQLException {
        // 1. First check if token exists and is not expired
        String checkTokenQuery = "SELECT id FROM user WHERE verification_token = ? AND is_verified = false";
        
        // 2. Then update user verification status
        String updateUserQuery = "UPDATE user SET is_verified = true WHERE id = ?";
        try (PreparedStatement Stmt = connection.prepareStatement(checkTokenQuery)) {
                Stmt.setString(1, token);
                ResultSet rs = Stmt.executeQuery();
                
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    // Update user verification status
                    try (PreparedStatement userStmt = connection.prepareStatement(updateUserQuery)) {
                        userStmt.setInt(1, userId);
                        int updated = userStmt.executeUpdate();
                        
                        if (updated == 1) {
                            connection.commit();
                            return true;
                        }
                    }
                }
                connection.rollback();
                return false;
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }    
    }

    public void sendPasswordReset(String email) throws SQLException {
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(1);

        String query = "UPDATE user SET reset_token = ?, reset_token_expires_at = ? WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, resetToken);
            stmt.setTimestamp(2, Timestamp.valueOf(expirationTime));
            stmt.setString(3, email);
            stmt.executeUpdate();
        }

	    
	    Properties props = System.getProperties();

	    props.put("mail.smtp.host", smtpHostServer);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(AutolinkEmail, AutolinkPassword);
			}
		};
		Session session = Session.getInstance(props, auth);

        String appLink = "Autolink://reset-password?token=" + resetToken;
        String webLink = "http://localhost:8080/reset-password?token=" + resetToken;

        String htmlContent = "Click the link below to reset your password: " + appLink + "\n"
            + "Or copy this link to your browser if the above doesn't work: " + webLink + "\n"
            + "This link will expire in 1 hour.";
		
		EmailUtil.sendEmail(session, email,"Reset Password", htmlContent);
    }

    public boolean resetPassword(String token, String newPassword) throws SQLException {
        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Token and new password cannot be null or empty");
        }

        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        String query = "UPDATE user SET password = ?, reset_token = NULL WHERE reset_token = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, hashedPassword);
            stmt.setString(2, token);
            
            int rowsAffected = stmt.executeUpdate();

            return rowsAffected == 1;
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


    public User getUserByVerificationToken(String verificationToken) throws SQLException{
        String req = "SELECT email, password FROM user WHERE verification_token=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) { // Remplacé cnx par connection
            ps.setString(1, verificationToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            }
        }
        return null;
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

    public boolean updateUserProfile(User user) throws SQLException {
        String sql = "UPDATE user SET name = ?, last_name = ?, phone = ?, email = ?, image_path = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLastName());
            stmt.setInt(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getImage_path());
            stmt.setInt(6, user.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    public boolean updateUserPassword(User user) throws SQLException {
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getPassword());
            stmt.setInt(2, user.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public boolean verifyPassword(User user, String password) {
        // Compare hashed password with input
        return BCrypt.checkpw(password, user.getPassword());
    }
    
    public boolean changePassword(User user, String newPassword) {
        // Update password in database
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        try {
            return updateUserPassword(user);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    
    public boolean createAccount(User user)  {
        String sql = "INSERT INTO `user`(`name`, `last_name`, `phone`, `email`, `password`, `role_id`, `created_at`, `is_verified`, `image_path`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLastName());
            stmt.setInt(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            // Encrypt the password using BCrypt
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            stmt.setString(5, hashedPassword);
            stmt.setInt(6, 2);
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            stmt.setBoolean(8, false);
            stmt.setString(9, user.getImage_path());

            int rowsAffected = stmt.executeUpdate();
            sendAccountVerification(user.getEmail());
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getAllUsersWithProfilePhotos() {
        List<User> users = new ArrayList<>();
        String query = "SELECT u.email, u.password, u.image_path FROM User u WHERE u.image_path IS NOT NULL";
        
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));
                    user.setImage_path(rs.getString("image_path"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Consider throwing a custom exception or returning empty list
        }
        return users;
    }
}
