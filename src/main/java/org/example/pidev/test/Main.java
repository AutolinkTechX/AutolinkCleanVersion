package org.example.pidev.test;

import org.example.pidev.utils.MyDatabase;

public class Main {
    public static void main(String[] args) {
        // Test de connexion à la base de données
        MyDatabase database = MyDatabase.getInstance();

        if (database.getMyConnection() != null) {
            System.out.println("Connexion réussie à la base de données !");
        } else {
            System.out.println("Échec de la connexion à la base de données.");
        }
    }
}


