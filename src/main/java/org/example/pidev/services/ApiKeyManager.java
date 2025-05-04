package org.example.pidev.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class ApiKeyManager {
    private static final Logger logger = Logger.getLogger(ApiKeyManager.class.getName());
    private static final String PREF_KEY = "HF_API_KEY";
    private static final String API_VALIDATION_URL = "https://huggingface.co/api/whoami";
    private static final Duration VALIDATION_TIMEOUT = Duration.ofSeconds(15);

    private final Preferences prefs;
    private final HttpClient httpClient;
/*
    public ApiKeyManager() {
        this.prefs = Preferences.userNodeForPackage(ApiKeyManager.class);
        this.httpClient = HttpClient.newHttpClient();

        // Solution temporaire pour tester - À RETIRER en production
        if (prefs.get(PREF_KEY, null) == null) {
            prefs.put(PREF_KEY, "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");
        }
    }
*/
    public ApiKeyManager() {
        this.prefs = Preferences.userNodeForPackage(ApiKeyManager.class);
        this.httpClient = HttpClient.newHttpClient();

        // À utiliser uniquement pour le développement
        if (prefs.get(PREF_KEY, null) == null) {
            prefs.put(PREF_KEY, "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");
        }
    }
    public void configureApiKey(String apiKey) {
       // if (apiKey == null) apiKey = "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";

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("La clé API ne peut pas être vide");
        }

        if (!apiKey.startsWith("hf_")) {
            throw new IllegalArgumentException("Format invalide. La clé doit commencer par 'hf_'");
        }

        try {
            if (!validateApiKey(apiKey)) {
                throw new IllegalArgumentException("Clé rejetée par Hugging Face");
            }
            prefs.put(PREF_KEY, apiKey);
            logger.info("Clé API enregistrée avec succès");
        } catch (Exception e) {
            logger.severe("Échec de configuration : " + e.getMessage());
            throw new RuntimeException("Erreur de configuration API", e);
        }
    }

    public String getApiKey() {
        return prefs.get(PREF_KEY, null); // Récupère la clé depuis les préférences
    }

    /*
    public boolean isApiKeyValid() {
        return true; // Force la validation pour tester
    }
*/
/*
    public boolean isApiKeyValid() {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        try {
            return validateApiKey(apiKey);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Échec de validation de la clé API", e);
            return false;
        }
    }

 */
    public boolean isApiKeyValid() {
        String key = getApiKey();
        // Vérifie que la clé existe et commence par hf_
        return key != null && !key.isEmpty() && key.startsWith("hf_");
    }


    private boolean validateApiKey(String apiKey) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_VALIDATION_URL))
                .header("Authorization", "Bearer " + apiKey)
                .timeout(VALIDATION_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }
}