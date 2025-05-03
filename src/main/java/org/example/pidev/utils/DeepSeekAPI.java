package org.example.pidev.utils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.*;


public class DeepSeekAPI {

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = ""; // À remplir avec votre clé
    private final Gson gson = new Gson();

    private final String SYSTEM_PROMPT =
            "Vous êtes l'assistant expert d'Autlink, une plateforme spécialisée dans la vente de pièces automobiles recyclées et la collecte de matériaux usagés.\n" +
                    "Votre mission est d'accompagner les clients en :\n" +
                    "- Expliquant nos services de recyclage et de vente de pièces reconditionnées\n" +
                    "- Mentionnant systématiquement la garantie de 6 mois offerte\n" +
                    "- Proposant des évaluations et diagnostics gratuits pour les pièces usagées\n" +
                    "- Demandant les spécifications nécessaires pour établir des devis précis\n" +
                    "- Mettant en avant les économies possibles par rapport aux pièces neuves\n\n" +
                     "Domaines d'expertise :\n" +
                    "1. Vente de pièces automobiles recyclées (pneus, pare-chocs, jantes, réservoirs, etc.)\n" +
                    "2. Collecte de matériaux recyclables (plastique, verre, caoutchouc)\n" +
                    "3. Reconditionnement et transformation de pièces automobiles\n" +
                    "4. Engagements environnementaux et certifications de recyclage\n\n" +
                    "Répondez toujours de manière chaleureuse et accueillante. Par exemple, si un utilisateur dit 'salut' ou 'bonjour', répondez par :\n" +
                    "'Salut et bienvenue chez Autlink ! Comment puis-je vous aider aujourd'hui ?"
            +"Si la question posée est en dehors de ces domaines, répondez uniquement par : 'Désolé, je ne peux pas répondre à cette question.";


    public String getResponseFromDeepSeek(String userInput) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JsonObject payload = new JsonObject();
            payload.addProperty("model", "deepseek/deepseek-r1:free");
            payload.addProperty("temperature", 0.7);
            payload.addProperty("max_tokens", 500);

            JsonArray messages = new JsonArray();

            // Message système
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", SYSTEM_PROMPT);
            messages.add(systemMessage);

            // Message utilisateur
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", userInput);
            messages.add(userMessage);

            payload.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://recyclotech.com")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return "[Erreur] Problème de connexion au service (Code " + response.statusCode() + ")";
            }

            String reply = parseApiResponse(response.body());

            // Si la réponse contient un message de refus, retourne-le tel quel
            if (reply.toLowerCase().contains("désolé, je ne peux pas répondre")) {
                return reply;
            }

            // Optionnel : tu peux aussi ajouter ici une vérification par mots-clés
            return reply;

        } catch (Exception e) {
            return "[Erreur] Service temporairement indisponible";
        }
    }


    private String parseApiResponse(String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            return jsonObject.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } catch (Exception e) {
            return "[Erreur] Format de réponse inattendu";
        }
    }

}