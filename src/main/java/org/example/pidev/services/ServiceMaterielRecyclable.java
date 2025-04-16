package org.example.pidev.services;

import org.example.pidev.Enum.StatutEnum;
import org.example.pidev.Enum.Type_materiel;
import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.MaterielRecyclable;
import org.example.pidev.utils.MyDatabase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



public class ServiceMaterielRecyclable implements  IService<MaterielRecyclable>{

    private Connection connection;

    public ServiceMaterielRecyclable(){
        connection = MyDatabase.getInstance().getMyConnection();
    }

    @Override
    public void ajouter(MaterielRecyclable materielRecyclable) throws SQLException {

        String sql = "INSERT INTO `materiel_recyclable`(`name`, `description`, `datecreation`, `type_materiel`, `image`, `statut`, `entreprise_id`) " +
                "VALUES ('" + materielRecyclable.getName() + "', '"
                + materielRecyclable.getDescription() + "', '"
                + materielRecyclable.getDateCreation() + "', '"
                + materielRecyclable.getType_materiel()+ "', '" // Utiliser .name() pour stocker l'enum en String
                + materielRecyclable.getImage() + "', '"
                + materielRecyclable.getStatut().name() + "', '" // ici on passe le statut en String
                + materielRecyclable.getEntreprise().getId() + "')";

        Statement stm = connection.createStatement();
        stm.executeUpdate(sql);
    }


    @Override
    public void modifier(MaterielRecyclable materielRecyclable) throws SQLException {

    }





    @Override
    public List<MaterielRecyclable> afficher() throws SQLException {
        List<MaterielRecyclable> materiaux = new ArrayList<>();

        String sql = "SELECT m.*, e.company_name AS companyname FROM materiel_recyclable m " +
                "JOIN entreprise e ON m.entreprise_id = e.id"; // 🔹 Jointure pour récupérer le nom de l'entreprise

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(sql);

        while (rs.next()) {
            // Récupération des valeurs depuis la base de données
            String name = rs.getString("name");
            String description = rs.getString("description");
            LocalDateTime dateCreation = rs.getTimestamp("datecreation").toLocalDateTime();
            String typeMaterielStr = rs.getString("type_materiel");
            String image = rs.getString("image");
            String statutStr = rs.getString("statut");
            String companyName = rs.getString("companyname"); // 🔹 Récupération du nom de l'entreprise

            // Conversion des types Enum
            Type_materiel typeMateriel = Type_materiel.valueOf(typeMaterielStr);
            StatutEnum statut = StatutEnum.valueOf(statutStr);

            // Création d'un objet Entreprise avec son nom
            Entreprise entreprise = new Entreprise(companyName);

            // Création de l'objet MaterielRecyclable
            MaterielRecyclable materiel = new MaterielRecyclable(
                    name, description, dateCreation, typeMateriel, image, statut, entreprise
            );

            // Ajout à la liste
            materiaux.add(materiel);
        }

        return materiaux;
    }


    @Override
    public void supprimer(int id) throws SQLException {

    }
}
