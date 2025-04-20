package org.example.pidev.services;

import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.Enum.Type_materiel;
import org.example.pidev.entities.Accord;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceAccord implements IService<Accord> {

    private Connection connection;

    public ServiceAccord() {
        connection = MyDatabase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(Accord accord) throws SQLException {
        // S'assurer que la connexion est valide
      //  connection = MyDatabase.getInstance().getMyConnection();

        // Récupérer d'abord le matériel avec son image
        String getMaterielSql = "SELECT * FROM materiel_recyclable WHERE id = " + accord.getMaterielRecyclable().getId();
        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(getMaterielSql);

        if (rs.next()) {
            // Mettre à jour l'objet MaterielRecyclable avec l'image
            String imagePath = rs.getString("image");
            if (imagePath != null && !imagePath.isEmpty()) {
                accord.getMaterielRecyclable().setImage(imagePath);
            }
        }

        // Créer l'accord avec date_reception NULL
        String sql = "INSERT INTO accord (date_creation, date_reception, quantity, output, materiel_recyclable_id, entreprise_id) " +
                "VALUES ('" + accord.getDateCreation() + "', " +
                "NULL, " + // date_reception est NULL par défaut
                accord.getQuantity() + ", " +
                (accord.getOutput() == null ? "NULL" : "'" + accord.getOutput() + "'") + ", " +
                accord.getMaterielRecyclable().getId() + ", " +
                accord.getEntreprise().getId() + ")";

        stm.executeUpdate(sql);
    }

    @Override
    public void modifier(Accord accord) throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        String sql = "UPDATE accord SET " +
                "date_creation = '" + accord.getDateCreation() + "', " +
                "date_reception = '" + accord.getDateReception() + "', " +
                "quantity = " + accord.getQuantity() + ", " +
                "output = '" + accord.getOutput() + "', " +
                "materiel_recyclable_id = " + accord.getMaterielRecyclable().getId() + ", " +
                "entreprise_id = " + accord.getEntreprise().getId() + " " +
                "WHERE id = " + accord.getId();

        Statement stm = connection.createStatement();
        stm.executeUpdate(sql);
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();
        // Requête paramétrée pour éviter les injections SQL
        String sql = "DELETE FROM accord WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, id); // Sécurité contre les injections SQL
            pst.executeUpdate();
        }
        // Le try-with-resources ferme automatiquement le PreparedStatement
    }

    @Override
    public List<Accord> afficher() throws SQLException {
        // S'assurer que la connexion est valide
      //  connection = MyDatabase.getInstance().getMyConnection();

        List<Accord> accords = new ArrayList<>();
        String sql = "SELECT a.*, m.id as materiel_id, m.name as materiel_name, m.description as materiel_description, " +
                "m.datecreation as materiel_datecreation, m.type_materiel as materiel_type, " +
                "m.image as materiel_image, m.statut as materiel_statut, " +
                "e.id as entreprise_id, e.company_name, e.email, e.phone " +
                "FROM accord a " +
                "JOIN materiel_recyclable m ON a.materiel_recyclable_id = m.id " +
                "JOIN entreprise e ON a.entreprise_id = e.id";
        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        while (rs.next()) {
            Accord accord = new Accord();
            accord.setId(rs.getInt("id"));
            accord.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());

            // Gestion de la date de réception qui peut être NULL
            java.sql.Timestamp dateReception = rs.getTimestamp("date_reception");
            if (dateReception != null) {
                accord.setDateReception(dateReception.toLocalDateTime());
            } else {
                accord.setDateReception(null);
            }

            accord.setQuantity(rs.getFloat("quantity"));
            accord.setOutput(rs.getString("output"));

            MaterielRecyclable materiel = new MaterielRecyclable();
            materiel.setId(rs.getInt("materiel_id"));
            materiel.setName(rs.getString("materiel_name"));
            materiel.setDescription(rs.getString("materiel_description"));
            materiel.setDateCreation(rs.getTimestamp("materiel_datecreation").toLocalDateTime());
            materiel.setType_materiel(Type_materiel.valueOf(rs.getString("materiel_type")));

            // Récupération de l'image du matériel
            String imagePath = rs.getString("materiel_image");
            if (imagePath != null && !imagePath.isEmpty()) {
                materiel.setImage(imagePath);
            }

            String statutStr = rs.getString("materiel_statut");
            if (statutStr != null) {
                materiel.setStatut(StatutEnum.valueOf(statutStr));
            }

            Entreprise entreprise = new Entreprise();
            entreprise.setId(rs.getInt("entreprise_id"));
            entreprise.setCompanyName(rs.getString("company_name"));
            entreprise.setEmail(rs.getString("email"));
            entreprise.setPhone(rs.getString("phone"));

            materiel.setEntreprise(entreprise);
            accord.setMaterielRecyclable(materiel);
            accord.setEntreprise(entreprise);
            accords.add(accord);
        }
        return accords;
    }

    public List<Accord> getAccordsByEntrepriseId(int entrepriseId) {
        // S'assurer que la connexion est valide
      //  connection = MyDatabase.getInstance().getMyConnection();

        List<Accord> accords = new ArrayList<>();
        String sql = "SELECT a.*, m.id as materiel_id, m.name as materiel_name, m.description as materiel_description, " +
                "m.datecreation as materiel_datecreation, m.type_materiel as materiel_type, " +
                "m.image as materiel_image, m.statut as materiel_statut, " +
                "e.id as entreprise_id, e.company_name, e.email, e.phone " +
                "FROM accord a " +
                "JOIN materiel_recyclable m ON a.materiel_recyclable_id = m.id " +
                "JOIN entreprise e ON a.entreprise_id = e.id " +
                "WHERE e.id = ?";

        try (Connection conn = MyDatabase.getInstance().getMyConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, entrepriseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Accord accord = new Accord();
                accord.setId(rs.getInt("id"));
                accord.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());

                java.sql.Timestamp dateReception = rs.getTimestamp("date_reception");
                accord.setDateReception(dateReception != null ? dateReception.toLocalDateTime() : null);

                accord.setQuantity(rs.getFloat("quantity"));
                accord.setOutput(rs.getString("output"));

                MaterielRecyclable materiel = new MaterielRecyclable();
                materiel.setId(rs.getInt("materiel_id"));
                materiel.setName(rs.getString("materiel_name"));
                materiel.setDescription(rs.getString("materiel_description"));
                materiel.setDateCreation(rs.getTimestamp("materiel_datecreation").toLocalDateTime());
                materiel.setType_materiel(Type_materiel.valueOf(rs.getString("materiel_type")));

                String imagePath = rs.getString("materiel_image");
                if (imagePath != null && !imagePath.isEmpty()) {
                    materiel.setImage(imagePath);
                }

                String statutStr = rs.getString("materiel_statut");
                if (statutStr != null) {
                    materiel.setStatut(StatutEnum.valueOf(statutStr));
                }

                Entreprise entreprise = new Entreprise();
                entreprise.setId(rs.getInt("entreprise_id"));
                entreprise.setCompanyName(rs.getString("company_name"));
                entreprise.setEmail(rs.getString("email"));
                entreprise.setPhone(rs.getString("phone"));

                materiel.setEntreprise(entreprise);
                accord.setMaterielRecyclable(materiel);
                accord.setEntreprise(entreprise);

                accords.add(accord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return accords;
    }


    public Accord getAccordById(String idAccord) {
        Accord accord = null;
        String query = "SELECT a.*, m.id as materiel_id, m.name as materiel_name, " +
                "m.description as materiel_description, m.date_creation as materiel_datecreation, " +
                "m.type_materiel as materiel_type, m.image as materiel_image, " +
                "m.statut as materiel_statut, e.id as entreprise_id, " +
                "e.company_name, e.email, e.phone " +
                "FROM accord a " +
                "JOIN materiel_recyclable m ON a.materiel_recyclable_id = m.id " +
                "JOIN entreprise e ON a.entreprise_id = e.id " +
                "WHERE a.id = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, idAccord);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                accord = new Accord();
                accord.setId(rs.getInt("id"));
                accord.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());

                if (rs.getTimestamp("date_reception") != null) {
                    accord.setDateReception(rs.getTimestamp("date_reception").toLocalDateTime());
                }

                // Créer l'objet MaterielRecyclable associé
                MaterielRecyclable materiel = new MaterielRecyclable();
                materiel.setId(rs.getInt("materiel_id"));
                materiel.setName(rs.getString("materiel_name"));
                materiel.setDescription(rs.getString("materiel_description"));
                materiel.setDateCreation(rs.getTimestamp("materiel_datecreation").toLocalDateTime());
                materiel.setType_materiel(Type_materiel.valueOf(rs.getString("materiel_type")));

                // Récupération de l'image du matériel
                String imagePath = rs.getString("materiel_image");
                if (imagePath != null && !imagePath.isEmpty()) {
                    materiel.setImage(imagePath);
                }

                String statutStr = rs.getString("materiel_statut");
                if (statutStr != null) {
                    materiel.setStatut(StatutEnum.valueOf(statutStr));
                }

                // Créer l'objet Entreprise associé
                Entreprise entreprise = new Entreprise();
                entreprise.setId(rs.getInt("entreprise_id"));
                entreprise.setCompanyName(rs.getString("company_name"));
                entreprise.setEmail(rs.getString("email"));
                entreprise.setPhone(rs.getString("phone"));

                materiel.setEntreprise(entreprise);
                accord.setMaterielRecyclable(materiel);
                accord.setEntreprise(entreprise);
            }

            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accord;
    }

    public Map<String, Integer> getNombreDemandesParClient(int entrepriseId) throws SQLException {
        Map<String, Integer> demandesParClient = new HashMap<>();
        String sql = "SELECT u.name, u.last_name, COUNT(*) as nombre_demandes " +
                "FROM accord a " +
                "JOIN materiel_recyclable m ON a.materiel_recyclable_id = m.id " +
                "JOIN user u ON m.user_id = u.id " +
                "WHERE m.entreprise_id = ? " +
                "GROUP BY u.name, u.last_name";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, entrepriseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String clientName = rs.getString("name") + " " + rs.getString("last_name");
                int nombreDemandes = rs.getInt("nombre_demandes");
                demandesParClient.put(clientName, nombreDemandes);
            }
        }
        return demandesParClient;
    }

    public double getTempsMoyenTraitement(int entrepriseId) throws SQLException {
        String sql = "SELECT AVG(TIMESTAMPDIFF(HOUR, a.date_creation, a.date_reception)) as temps_moyen " +
                "FROM accord a " +
                "JOIN materiel_recyclable m ON a.materiel_recyclable_id = m.id " +
                "WHERE m.entreprise_id = ? " +
                "AND a.date_reception IS NOT NULL " +
                "AND m.statut IN ('valide', 'refuse')";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, entrepriseId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("temps_moyen");
            }
        }
        return 0.0;
    }

    public Map<String, Long> getMateriauxEnAttenteLongue(int entrepriseId) throws SQLException {
        Map<String, Long> materiauxEnAttente = new HashMap<>();
        String sql = "SELECT m.name, TIMESTAMPDIFF(HOUR, m.datecreation, NOW()) as temps_attente " +
                "FROM materiel_recyclable m " +
                "WHERE m.entreprise_id = ? " +
                "AND m.statut = 'en_attente' " +
                "AND TIMESTAMPDIFF(HOUR, m.datecreation, NOW()) > 24 " + // Plus de 24h en attente
                "ORDER BY temps_attente DESC " +
                "LIMIT 10"; // Limiter aux 10 plus longs temps d'attente

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, entrepriseId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String nomMateriel = rs.getString("name");
                long tempsAttente = rs.getLong("temps_attente");
                materiauxEnAttente.put(nomMateriel, tempsAttente);
            }
        }
        return materiauxEnAttente;
    }


       public int getNombreDemandesParEntreprise(int entrepriseId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accord WHERE entreprise_id = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, entrepriseId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }



    public Map<String, Integer> getNombreDemandesParStatut(int idEntrepriseConnectee) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT m.statut, COUNT(*) as total " +
                "FROM materiel_recyclable m " +
                "JOIN accord a ON a.materiel_recyclable_id  = m.id " +
                "WHERE m.entreprise_id = ? " +
                "GROUP BY m.statut";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, idEntrepriseConnectee);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            result.put(rs.getString("statut"), rs.getInt("total"));
        }

        return result;
    }



}

