package org.example.pidev.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationService {
    private static final Map<String, String> translationCache = new HashMap<>();
    private static final String API_URL = "https://libretranslate.com/translate";
    private static final String API_KEY = ""; // Laissé vide pour LibreTranslate public

    // Dictionnaire de fallback
    private static final Map<String, String> frenchToEnglish = Map.of(
            "Détails de l'article", "Article details",
            "Catégorie", "Category",
            "Description", "Description",
            "Prix", "Price",
            "Stock disponible", "Available stock"
    );

    /*
    public static String translate(String text, String targetLang) {
        if (text == null || text.isEmpty()) return text;

        // Vérifier le cache
        String cacheKey = text + "|" + targetLang;
        if (translationCache.containsKey(cacheKey)) {
            return translationCache.get(cacheKey);
        }

        // Essayer l'API
        try {
            String translated = translateWithAPI(text, "fr", targetLang);
            translationCache.put(cacheKey, translated);
            return translated;
        } catch (IOException e) {
            System.err.println("API translation failed, using fallback: " + e.getMessage());
            // Fallback au dictionnaire local
            return frenchToEnglish.getOrDefault(text, text);
        }
    }
*/

    public static String translate(String text, String targetLang) {
        if (text == null || text.isEmpty()) return text;

        String cacheKey = text + "|" + targetLang;
        if (translationCache.containsKey(cacheKey)) {
            return translationCache.get(cacheKey);
        }

        // Essayer l'API
        try {
            String translated = translateWithAPI(text, "fr", targetLang);
            if (!translated.isEmpty()) {
                translationCache.put(cacheKey, translated);
                return translated;
            }
        } catch (Exception e) {
            System.err.println("Translation API error: " + e.getMessage());
        }

        // Fallback 1: Dictionnaire local pour les phrases courantes
        String dictTrans = frenchToEnglish.get(text);
        if (dictTrans != null) {
            return dictTrans;
        }

        // Fallback 2: Pour les longues descriptions, retourner l'original avec un avertissement
        if (text.length() > 30) {
            return text + " [Translation unavailable]";
        }

        return text;
    }

    private static String translateWithAPI(String text, String sourceLang, String targetLang) throws IOException {
        String urlStr = String.format(
                "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s",
                URLEncoder.encode(text, "UTF-8"),
                sourceLang,
                targetLang);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            // Analyse la réponse JSON différente de MyMemory
            return response.toString().split("\"translatedText\":\"")[1].split("\"")[0];
        }
    }
}