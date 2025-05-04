package org.example.pidev.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AIGenerationService {
    private static final Logger logger = Logger.getLogger(AIGenerationService.class.getName());
    private static final String SIMPLE_MODEL = "gpt2"; // Modèle plus simple et rapide
    private static final String HF_API_URL = "https://api-inference.huggingface.co/models/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final ApiKeyManager apiKeyManager;

    public AIGenerationService(ApiKeyManager apiKeyManager) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.mapper = new ObjectMapper();
        this.apiKeyManager = apiKeyManager;
    }

    public String generateSimpleDescription(String productName) {
        try {
            if (!apiKeyManager.isApiKeyValid()) {
                return getFallbackDescription(productName);
            }

            String prompt = "Génère une description produit simple en 1-2 phrases pour: " + productName;
            String jsonPayload = String.format("{\"inputs\":\"%s\", \"parameters\":{\"max_length\":100}}",
                    prompt.replace("\"", "\\\""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HF_API_URL + SIMPLE_MODEL))
                    .header("Authorization", "Bearer " + apiKeyManager.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = mapper.readTree(response.body());
                if (rootNode.has("generated_text")) {
                    return rootNode.get("generated_text").asText()
                            .replace(prompt, "")
                            .trim();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Erreur de génération: " + e.getMessage());
        }

        return getFallbackDescription(productName);
    }

    private String getFallbackDescription(String productName) {
        return "Produit " + productName + " de haute qualité pour vos besoins quotidiens.";
    }
}