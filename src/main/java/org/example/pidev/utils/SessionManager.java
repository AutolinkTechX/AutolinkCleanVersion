package org.example.pidev.utils;

import org.example.pidev.entities.Entreprise;
import org.example.pidev.entities.User;
import org.example.pidev.services.UserService;
import org.example.pidev.services.EntrepriseService;

import java.sql.SQLException;

import java.util.prefs.*;

public class SessionManager {
    private static User currentUser;
    private static Entreprise currentEntreprise;
    private static String currentUserType; // "USER" or "ENTREPRISE"

    private static final String PREFS_NODE = "org.example.pidev";
    private static final String REMEMBER_ME = "rememberMe";
    private static final String USER_TYPE = "userType";
    private static final String USER_ID = "userId";
    private static final String USER_EMAIL = "userEmail";
    private static final String ENTREPRISE_ID = "entrepriseId";
    private static final String ENTREPRISE_EMAIL = "entrepriseEmail";

    public static boolean restoreSession() throws SQLException {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        if (!prefs.getBoolean(REMEMBER_ME, false)) {
            return false;
        }

        String userType = prefs.get(USER_TYPE, null);
        UserService userService = new UserService();
        EntrepriseService entrepriseService = new EntrepriseService();

        if ("USER".equals(userType)) {
            int userId = prefs.getInt(USER_ID, -1);
            String email = prefs.get(USER_EMAIL, null);
            
            if (userId != -1 && email != null) {
                User user = userService.getUserById(userId);
                if (user != null && email.equals(user.getEmail())) {
                    currentUser = user;
                    currentUserType = "USER";
                    return true;
                }
            }
        }
        else if ("ENTREPRISE".equals(userType)) {
            int entrepriseId = prefs.getInt(ENTREPRISE_ID, -1);
            String email = prefs.get(ENTREPRISE_EMAIL, null);
            
            if (entrepriseId != -1 && email != null) {
                Entreprise entreprise = entrepriseService.getById(entrepriseId);
                if (entreprise != null && email.equals(entreprise.getEmail())) {
                    currentEntreprise = entreprise;
                    currentUserType = "ENTREPRISE";
                    return true;
                }
            }
        }
        
        // Clear invalid session
        clearSession();
        return false;
    }

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
        
        // Completely remove the preferences node
        try {
            Preferences.userRoot().node(PREFS_NODE).removeNode();
        } catch (BackingStoreException e) {
            System.err.println("Error clearing session preferences: " + e.getMessage());
        }
        
        System.out.println("Session cleared successfully");
    }

    public static void saveSession(boolean rememberMe) throws BackingStoreException {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        System.out.println("Saving session - rememberMe: " + rememberMe);
        System.out.println("Current user: " + (currentUser != null ? currentUser.getEmail() : "null"));
        System.out.println("Current entreprise: " + (currentEntreprise != null ? currentEntreprise.getEmail() : "null"));
            
        if (rememberMe) {
            prefs.putBoolean(REMEMBER_ME, true);
            
            if (currentUser != null) {
                prefs.put(USER_TYPE, "USER");
                prefs.putInt(USER_ID, currentUser.getId());
                prefs.put(USER_EMAIL, currentUser.getEmail());
            } else if (currentEntreprise != null) {
                prefs.put(USER_TYPE, "ENTREPRISE");
                prefs.putInt(ENTREPRISE_ID, currentEntreprise.getId());
                prefs.put(ENTREPRISE_EMAIL, currentEntreprise.getEmail());
            }
        } else {
            prefs.removeNode();
        }
    }

    public static boolean hasSavedSession() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.getBoolean(REMEMBER_ME, false);
    }

    public static String getSavedUserType() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.get(USER_TYPE, null);
    }

    public static int getSavedUserId() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.getInt(USER_ID, -1);
    }

    public static String getSavedUserEmail() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.get(USER_EMAIL, null);
    }

    public static int getSavedEntrepriseId() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.getInt(ENTREPRISE_ID, -1);
    }

    public static String getSavedEntrepriseEmail() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        return prefs.get(ENTREPRISE_EMAIL, null);
    }
    
}