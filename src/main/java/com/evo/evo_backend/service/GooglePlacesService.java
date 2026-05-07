package com.evo.evo_backend.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.Competitor;

@Service
public class GooglePlacesService {

    private static final Logger log = Logger.getLogger(GooglePlacesService.class.getName());

    @Value("${google.places.api.key:}")
    private String apiKey;

    private static final String PLACES_URL = "https://places.googleapis.com/v1/places:searchText";

    // =========================
    // MAIN METHOD
    // =========================
    public List<Competitor> getLocalCompetitors(String keyword, String location) {

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GOOGLE_PLACES_API_KEY")) {
            log.warning("Google Places API key not configured");
            return List.of();
        }

        // 1. Clean and validate the keyword before processing
        String cleanKeyword = sanitizeKeyword(keyword);
        
        if (!isValidKeyword(cleanKeyword)) {
            log.warning("Skipping Places API call — invalid or low-quality keyword: " + cleanKeyword);
            return List.of();
        }

        // 2. Build a high-intent local query
        String query = buildQuery(cleanKeyword, location);
        log.info("Google Places query: " + query);

        try {
            String responseBody = callPlacesApi(query);

            if (responseBody == null || responseBody.isBlank() || responseBody.equals("{}")) {
                log.warning("Google Places returned empty response for: " + query);
                return List.of();
            }

            return parseResponse(responseBody);

        } catch (Exception e) {
            log.warning("Google Places API error: " + e.getMessage());
            return List.of();
        }
    }

    // =========================
    // SANITIZE KEYWORD (NEW)
    // =========================
    private String sanitizeKeyword(String keyword) {
        if (keyword == null) return "";
        // Remove common filler words that dilute local search intent
        return keyword.replaceAll("(?i)\\b(the|a|an|top|best|engine|in India|services|list of)\\b", "").trim();
    }

    // =========================
    // BUILD QUERY (IMPROVED)
    // =========================
    private String buildQuery(String keyword, String location) {
        String kw = keyword.trim();
        
        // If we have a location (e.g., Coimbatore), force it into the search query
        if (location != null && !location.isBlank() && !location.equalsIgnoreCase("unknown")) {
            String loc = location.trim();
            if (!kw.toLowerCase().contains(loc.toLowerCase())) {
                // Returns "Organic Cafe Coimbatore" instead of just "Organic Cafe"
                return kw + " " + loc;
            }
        }
        return kw;
    }

    // =========================
    // VALIDATION (TOUGHER)
    // =========================
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.isBlank() || keyword.length() < 3 || keyword.length() > 60) return false;

        String lower = keyword.toLowerCase();
        
        // Block meta-keywords that come from SEO tool errors or AI hallucinations
        return !lower.contains("error")    &&
               !lower.contains("429")      &&
               !lower.contains("status")   &&
               !lower.contains("undefined")&&
               !lower.contains("null")     &&
               !lower.contains("google")   &&
               !lower.contains("search")   && // Prevents "search engine" queries
               !lower.contains("website")  &&
               !lower.contains("analysis");
    }

    // =========================
    // API CALL (STABLE)
    // =========================
    private String callPlacesApi(String query) throws Exception {
        URL url = new URL(PLACES_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Goog-Api-Key", apiKey);

        // Required FieldMask for V1 API
        conn.setRequestProperty("X-Goog-FieldMask",
                "places.displayName,places.rating,places.userRatingCount," +
                "places.formattedAddress,places.websiteUri,places.primaryType," +
                "places.businessStatus");

        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        JSONObject body = new JSONObject();
        body.put("textQuery", query);
        body.put("maxResultCount", 5); // Kept at 5 for performance
        body.put("languageCode", "en");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            return null;
        }

        return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    // =========================
    // PARSE RESPONSE
    // =========================
    private List<Competitor> parseResponse(String responseBody) {
        List<Competitor> competitors = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(responseBody);
            if (!root.has("places")) return competitors;

            JSONArray places = root.getJSONArray("places");

            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);

                // Skip businesses that are closed
                String status = place.optString("businessStatus", "OPERATIONAL");
                if (status.equals("CLOSED_PERMANENTLY")) continue;

                String name = "Unknown";
                if (place.has("displayName")) {
                    name = place.getJSONObject("displayName").optString("text", "Unknown");
                }

                String address = place.optString("formattedAddress", "");
                String city = extractCity(address);

                // Shorten address for UI
                if (address.length() > 60) {
                    String[] parts = address.split(",");
                    if (parts.length >= 2) address = parts[0].trim() + ", " + parts[1].trim();
                }

                Competitor competitor = new Competitor();
                competitor.setName(name);
                competitor.setRating(place.optDouble("rating", 0.0));
                competitor.setReviewCount(place.optInt("userRatingCount", 0));
                competitor.setAddress(address);
                competitor.setWebsite(place.optString("websiteUri", ""));
                competitor.setType(place.optString("primaryType", "Business"));
                competitor.setCity(city);

                competitors.add(competitor);
            }
        } catch (Exception e) {
            log.warning("Failed to parse: " + e.getMessage());
        }
        return competitors;
    }

    private String extractCity(String address) {
        if (address == null || address.isBlank()) return "India";
        try {
            String[] parts = address.split(",");
            if (parts.length >= 3) {
                // Usually City is the 3rd last element in formatted addresses
                return parts[parts.length - 3].trim();
            }
        } catch (Exception ignored) {}
        return "Local";
    }
}