package org.example.pidev.services;



import org.example.pidev.entities.TaskWorkshop;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceTaskWorkshop {
    private Connection connection;

    public ServiceTaskWorkshop() {
        connection = MyDatabase.getInstance().getMyConnection();
    }

    // CREATE
    public void addTask(TaskWorkshop task) {
        String sql = "INSERT INTO task_workshop (nom, description, starts_at, ends_at, status, workshop_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Log des paramètres
            System.out.println("Ajout tâche : " + task.getNom() + " pour workshop " + task.getWorkshopId());

            statement.setString(1, task.getNom());
            statement.setString(2, task.getDescription());
            statement.setTimestamp(3, task.getStartsAt());
            statement.setTimestamp(4, task.getEndsAt());
            statement.setString(5, task.getStatus());
            statement.setInt(6, task.getWorkshopId());

            // Exécution
            int affectedRows = statement.executeUpdate();
            System.out.println("Lignes affectées : " + affectedRows); // Doit être 1

            // Commit explicite si nécessaire
            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            // Récupération de l'ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    task.setId(generatedKeys.getInt(1));
                    System.out.println("Tâche ajoutée avec ID: " + task.getId());
                }
            }
        } catch (SQLException e) {
            System.err.println("Échec de l'ajout : " + e.getMessage());
            e.printStackTrace();
            try {
                connection.rollback(); // Annulation en cas d'erreur
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // READ ALL
    public List<TaskWorkshop> getTasksForWorkshop(int workshopId) {
        List<TaskWorkshop> tasks = new ArrayList<>();
        String sql = "SELECT * FROM task_workshop WHERE workshop_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, workshopId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                TaskWorkshop task = new TaskWorkshop(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getTimestamp("starts_at"),
                        rs.getTimestamp("ends_at"),
                        rs.getString("status"),
                        rs.getInt("workshop_id")
                );
                tasks.add(task);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tasks;
    }

    // READ BY ID
    public TaskWorkshop getTaskById(int id) {
        String sql = "SELECT * FROM task_workshop WHERE id = ?";
        TaskWorkshop task = null;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                task = new TaskWorkshop(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("description"),
                        rs.getTimestamp("starts_at"),
                        rs.getTimestamp("ends_at"),
                        rs.getString("status"),
                        rs.getInt("workshop_id")
                );
                System.out.println("Tâche trouvée avec l'ID: " + id);
            } else {
                System.out.println("Aucune tâche trouvée avec l'ID: " + id);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la tâche : " + e.getMessage());
        }
        return task;
    }

    // UPDATE
    public void updateTask(TaskWorkshop task) {
        String sql = "UPDATE task_workshop SET nom = ?, description = ?, starts_at = ?, ends_at = ?, status = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, task.getNom());
            stmt.setString(2, task.getDescription());
            stmt.setTimestamp(3, task.getStartsAt());
            stmt.setTimestamp(4, task.getEndsAt());
            stmt.setString(5, task.getStatus());
            stmt.setInt(6, task.getId());

            int rowsUpdated = stmt.executeUpdate();
            connection.commit();
            if (rowsUpdated > 0) {
                System.out.println("Tâche mise à jour avec succès ! ID: " + task.getId());
            } else {
                System.out.println("Aucune tâche mise à jour. ID peut-être invalide.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la tâche : " + e.getMessage());
        }
    }

    // DELETE
    public void deleteTask(int id) {
        String sql = "DELETE FROM task_workshop WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Tâche supprimée avec succès ! ID: " + id);
            } else {
                System.out.println("Aucune tâche supprimée. ID peut-être invalide.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la tâche : " + e.getMessage());
        }
    }
}