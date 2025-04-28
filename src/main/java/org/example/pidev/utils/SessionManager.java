package org.example.pidev.utils;

import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.User;


public class SessionManager {
    private static User currentUser;
    private static Entreprise currentEntreprise;
    private static String currentUserType; // "USER" or "ENTREPRISE"



    public static void setCurrentUser(User user) {
        currentUser = user;
        currentEntreprise = null;
        currentUserType = "USER";
    }

    public static void setCurrentEntreprise(Entreprise entreprise) {
        currentEntreprise = entreprise;
        currentUser = null;
        currentUserType = "ENTREPRISE";
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static Entreprise getCurrentEntreprise() {
        return currentEntreprise;
    }

    public static String getCurrentUserType() {
        return currentUserType;
    }

    public static void clearSession() {
        currentUser = null;
        currentEntreprise = null;
        currentUserType = null;
        
        System.out.println("Session cleared successfully");
    }
    
}