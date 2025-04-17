package org.example.pidev.services;

import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.Enum.Type_materiel;

import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.entities.User;
import org.example.pidev.entities.Role;
import org.example.pidev.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServiceMaterielRecyclable implements  IService<MaterielRecyclable>{

    private Connection connection;

    public ServiceMaterielRecyclable(){
        connection = MyDatabase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(MaterielRecyclable materielRecyclable) throws SQLException {
        // Vérifier que l'entreprise existe
        if (materielRecyclable.getEntreprise() == null) {
            throw new SQLException("L'entreprise ne peut pas être null");
        }

        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        String sql = "INSERT INTO materiel_recyclable (name, description, datecreation, type_materiel, image, statut, entreprise_id, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, materielRecyclable.getName());
        pstmt.setString(2, materielRecyclable.getDescription());
        pstmt.setTimestamp(3, java.sql.Timestamp.valueOf(materielRecyclable.getDateCreation())); // 👈 conversion ici
        pstmt.setString(4, materielRecyclable.getType_materiel().toString());
        pstmt.setString(5, materielRecyclable.getImage());
        pstmt.setString(6, materielRecyclable.getStatut().name());
        pstmt.setInt(7, materielRecyclable.getEntreprise().getId());
        pstmt.setInt(8, materielRecyclable.getUser().getId());

        pstmt.executeUpdate();
    }


    @Override
    public void modifier(MaterielRecyclable materielRecyclable) throws SQLException {
        String sql = "UPDATE materiel_recyclable SET "
                + "name = ?, "
                + "description = ?, "
                + "type_materiel = ?, "
                + "image = ?, "
                + "statut = ?, "
                + "entreprise_id = ? "
                + "WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getMyConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            // Paramétrage sécurisé des valeurs
            pst.setString(1, materielRecyclable.getName());
            pst.setString(2, materielRecyclable.getDescription());
            pst.setString(3, materielRecyclable.getType_materiel().name());
            pst.setString(4, materielRecyclable.getImage());
            pst.setString(5, materielRecyclable.getStatut().name());
            pst.setInt(6, materielRecyclable.getEntreprise().getId());
            pst.setInt(7, materielRecyclable.getId());

            pst.executeUpdate();
        }
    }

    @Override
    public List<MaterielRecyclable> afficher() throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        List<MaterielRecyclable> materiels = new ArrayList<>();
        String sql = "SELECT m.*, e.id as entreprise_id, e.company_name, e.email as entreprise_email, e.phone as entreprise_phone, " +
                "u.id as user_id, u.name as user_name, u.last_name, u.email as user_email, u.phone as user_phone, u.role_id " +
                "FROM materiel_recyclable m " +
                "LEFT JOIN entreprise e ON m.entreprise_id = e.id " +
                "LEFT JOIN user u ON m.user_id = u.id";
        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        while (rs.next()) {
            MaterielRecyclable materiel = new MaterielRecyclable();
            materiel.setId(rs.getInt("id"));
            materiel.setName(rs.getString("name"));
            materiel.setDescription(rs.getString("description"));
            materiel.setDateCreation(rs.getTimestamp("datecreation").toLocalDateTime());
            materiel.setType_materiel(Type_materiel.valueOf(rs.getString("type_materiel")));
            materiel.setImage(rs.getString("image"));
            materiel.setStatut(StatutEnum.valueOf(rs.getString("statut")));

            // Créer et configurer l'entreprise
            Entreprise entreprise = new Entreprise();
            int entrepriseId = rs.getInt("entreprise_id");
            if (!rs.wasNull()) {
                entreprise.setId(entrepriseId);
                String companyName = rs.getString("company_name");
                entreprise.setCompanyName(companyName);
                entreprise.setEmail(rs.getString("entreprise_email"));
                entreprise.setPhone(rs.getString("entreprise_phone"));
                materiel.setEntreprise(entreprise);
            }

            materiels.add(materiel);
        }
        return materiels;
    }

    public MaterielRecyclable getMaterielByName(String name) throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        String sql = "SELECT m.*, e.id as entreprise_id, e.company_name, e.email as entreprise_email, e.phone as entreprise_phone, " +
                "u.id as user_id, u.name as user_name, u.last_name, u.email as user_email, u.phone as user_phone, u.role_id " +
                "FROM materiel_recyclable m " +
                "JOIN entreprise e ON m.entreprise_id = e.id " +
                "JOIN user u ON m.user_id = u.id " +
                "WHERE m.name = '" + name + "'";
        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        if (rs.next()) {
            MaterielRecyclable materiel = new MaterielRecyclable();
            materiel.setId(rs.getInt("id"));
            materiel.setName(rs.getString("name"));
            materiel.setDescription(rs.getString("description"));
            materiel.setDateCreation(rs.getTimestamp("datecreation").toLocalDateTime());
            materiel.setType_materiel(Type_materiel.valueOf(rs.getString("type_materiel")));
            String imagePath = rs.getString("image");
            if (imagePath != null && !imagePath.isEmpty()) {
                materiel.setImage(imagePath);
            }
            materiel.setStatut(StatutEnum.valueOf(rs.getString("statut")));

            Entreprise entreprise = new Entreprise();
            entreprise.setId(rs.getInt("entreprise_id"));
            entreprise.setCompanyName(rs.getString("company_name"));
            entreprise.setEmail(rs.getString("entreprise_email"));
            entreprise.setPhone(rs.getString("entreprise_phone"));
            materiel.setEntreprise(entreprise);

            User user = new User();
            user.setId(rs.getInt("user_id"));
            user.setName(rs.getString("user_name"));
            user.setLastName(rs.getString("last_name"));
            user.setEmail(rs.getString("user_email"));
            /* user.setPhone(rs.getString("user_phone"));*/
            materiel.setUser(user);

            return materiel;
        }
        return null;
    }

    @Override
    public void supprimer(int id) throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        String sql = "DELETE FROM materiel_recyclable WHERE id = " + id;
        Statement stm = connection.createStatement();
        stm.executeUpdate(sql);
    }

    public List<MaterielRecyclable> afficherParUtilisateur(int userId) throws SQLException {
        // S'assurer que la connexion est valide
        connection = MyDatabase.getInstance().getMyConnection();

        List<MaterielRecyclable> materiels = new ArrayList<>();
        String sql = "SELECT m.*, e.id as entreprise_id, e.company_name, e.email as entreprise_email, e.phone as entreprise_phone, " +
                "u.id as user_id, u.name as user_name, u.last_name, u.email as user_email, u.phone as user_phone, u.role_id " +
                "FROM materiel_recyclable m " +
                "LEFT JOIN entreprise e ON m.entreprise_id = e.id " +
                "LEFT JOIN user u ON m.user_id = u.id " +
                "WHERE m.user_id = " + userId;

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        while (rs.next()) {
            MaterielRecyclable materiel = new MaterielRecyclable();
            materiel.setId(rs.getInt("id"));
            materiel.setName(rs.getString("name"));
            materiel.setDescription(rs.getString("description"));
            materiel.setDateCreation(rs.getTimestamp("datecreation").toLocalDateTime());
            materiel.setType_materiel(Type_materiel.valueOf(rs.getString("type_materiel")));
            materiel.setImage(rs.getString("image"));
            materiel.setStatut(StatutEnum.valueOf(rs.getString("statut")));

            // Créer et configurer l'entreprise
            Entreprise entreprise = new Entreprise();
            int entrepriseId = rs.getInt("entreprise_id");
            if (!rs.wasNull()) {
                entreprise.setId(entrepriseId);
                String companyName = rs.getString("company_name");
                entreprise.setCompanyName(companyName);
                entreprise.setEmail(rs.getString("entreprise_email"));
                entreprise.setPhone(rs.getString("entreprise_phone"));
                materiel.setEntreprise(entreprise);
            }

            // Créer et configurer l'utilisateur
           /* User user = new User();
            user.setId(rs.getInt("user_id"));
            user.setName(rs.getString("user_name"));
            user.setLastName(rs.getString("last_name"));
            user.setEmail(rs.getString("user_email"));
            materiel.setUser(user);*/

            materiels.add(materiel);
        }
        return materiels;
    }




    /*ce code est ajouté pour la statique**/
    /*methode pour statique*/
    public int getNombreTotalDemandes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM materiel_recyclable";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    public Map<String, Integer> getNombreDemandesParStatut() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT statut, COUNT(*) as total FROM materiel_recyclable GROUP BY statut";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            result.put(rs.getString("statut"), rs.getInt("total"));
        }
        return result;
    }

    public Map<String, Integer> getNombreDemandesParType() throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT type_materiel, COUNT(*) as total FROM materiel_recyclable GROUP BY type_materiel";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            result.put(rs.getString("type_materiel"), rs.getInt("total"));
        }
        return result;
    }



}
