package org.example.pidev.services;



import org.example.pidev.entities.Workshop;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceWorkshop {
    private Connection connection;

    public ServiceWorkshop() {
        connection = MyDatabase.getInstance().getMyConnection();
    }

    public void addWorkshop(Workshop workshop) {
        String sql = "INSERT INTO workshop (name, description, starts_at, ends_at, location, image, price, available_places, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, workshop.getName());
            stmt.setString(2, workshop.getDescription());
            stmt.setTimestamp(3, workshop.getStartsAt());
            stmt.setTimestamp(4, workshop.getEndsAt());
            stmt.setString(5, workshop.getLocation());
            stmt.setString(6, workshop.getImage());
            stmt.setDouble(7, workshop.getPrice());
            stmt.setInt(8, workshop.getAvailablePlaces());
            stmt.setObject(9, workshop.getUser_id() == 0 ? null : workshop.getUser_id(), Types.INTEGER);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating workshop failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    workshop.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating workshop failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<Workshop> getAllWorkshops() {
        List<Workshop> workshops = new ArrayList<>();
        String sql = "SELECT * FROM workshop ORDER BY starts_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Workshop w = new Workshop(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("starts_at"),
                        rs.getTimestamp("ends_at"),
                        rs.getString("location"),
                        rs.getString("image"),
                        rs.getDouble("price"),
                        rs.getInt("available_places"),
                        rs.getInt("user_id")
                );
                if (rs.wasNull()) {
                    w.setUser_id(0);
                }
                workshops.add(w);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des workshops : " + e.getMessage());
            throw new RuntimeException(e);
        }
        return workshops;
    }

    public Workshop getWorkshopById(int id) {
        String sql = "SELECT * FROM workshop WHERE id = ?";
        Workshop workshop = null;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                workshop = new Workshop(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("starts_at"),
                        rs.getTimestamp("ends_at"),
                        rs.getString("location"),
                        rs.getString("image"),
                        rs.getDouble("price"),
                        rs.getInt("available_places"),
                        rs.getInt("user_id")
                );
                if (rs.wasNull()) {
                    workshop.setUser_id(0);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du workshop : " + e.getMessage());
            throw new RuntimeException(e);
        }
        return workshop;
    }

    public void updateWorkshop(Workshop workshop) {
        String sql = "UPDATE workshop SET name = ?, description = ?, starts_at = ?, ends_at = ?, " +
                "location = ?, image = ?, price = ?, available_places = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, workshop.getName());
            stmt.setString(2, workshop.getDescription());
            stmt.setTimestamp(3, workshop.getStartsAt());
            stmt.setTimestamp(4, workshop.getEndsAt());
            stmt.setString(5, workshop.getLocation());
            stmt.setString(6, workshop.getImage());
            stmt.setDouble(7, workshop.getPrice());
            stmt.setInt(8, workshop.getAvailablePlaces());
            stmt.setObject(9, workshop.getUser_id() == 0 ? null : workshop.getUser_id(), Types.INTEGER);
            stmt.setInt(10, workshop.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Updating workshop failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteWorkshop(int id) {
        String sql = "DELETE FROM workshop WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted == 0) {
                throw new SQLException("Deleting workshop failed, no rows affected.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}