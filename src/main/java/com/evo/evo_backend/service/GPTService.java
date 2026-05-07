package com.evo.evo_backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service("gptService")
public class GPTService {

    /**
     * GROQ CONFIGURATION (2026)
     * llama-3.3-70b-versatile: High intelligence for complex analysis.
     * llama-3.1-8b-instant: Faster for simple text transformations.
     */
    private static final String MODEL_ID = "llama-3.3-70b-versatile";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final List<String> apiKeys;
    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public GPTService(@Value("${groq.api.keys}") String keys) {
        if (keys == null || keys.isBlank()) {
            throw new RuntimeException("CRITICAL: API keys missing in application.properties");
        }
        
        // Clean keys of any extra spaces or semi-colons from the property file
        this.apiKeys = Arrays.stream(keys.split(","))
                             .map(String::trim)
                             .map(k -> k.replaceAll("[;\\s]", "")) 
                             .filter(key -> !key.isEmpty())
                             .collect(Collectors.toList());
        
        System.out.println("[EVO-LOG] Groq Service initialized with " + apiKeys.size() + " key(s).");
    }

    private String getActiveKey() {
        return apiKeys.get(currentKeyIndex.get() % apiKeys.size());
    }

    private void rotateKey() {
        if (apiKeys.size() > 1) {
            int nextIndex = (currentKeyIndex.get() + 1) % apiKeys.size();
            currentKeyIndex.set(nextIndex);
            System.out.println("[EVO-LOG] Rotating Groq Key to Index: " + nextIndex);
        }
    }

    // ============================================================
    // 🔥 MASTER PROMPT METHODS
    // ============================================================

    /**
     * Used by: AI Perception Simulator
     * Requirement: Returns a JSON object for the frontend to parse.
     */
    @Cacheable(value = "groq_unified_analysis", key = "@gptService.generateHash(#prompt)")
    public String getUnifiedAnalysis(String prompt) {
        System.out.println("[EVO-LOG] Groq.getUnifiedAnalysis() triggered.");
        String systemMsg = "You are a JSON-only response engine. Return valid JSON only. No prose or markdown.";
        return executeWithRotation(systemMsg, prompt, true);
    }

    /**
     * Used by: Semantic Content Transformer
     * Requirement: Returns plain text optimized for AI search engines.
     */
    @Cacheable(value = "groq_ai_analysis", key = "@gptService.generateHash(#content)")
    public String analyzeContent(String content) {
        if (content == null || content.isBlank()) return "No content provided";
        System.out.println("[EVO-LOG] Groq.analyzeContent() triggered.");
        
        String truncated = content.length() > 3000 ? content.substring(0, 3000) : content;
        String systemMsg = "You are an expert AI Semantic SEO Analyst.";
        String prompt = "Rewrite the following content to be 'AI-Native'. Remove marketing fluff, " +
                        "focus on entities, facts, and clear relationship structures that an LLM can parse easily. " +
                        "Return only the rewritten text.\n\nContent:\n" + truncated;

        return executeWithRotation(systemMsg, prompt, false);
    }

    /**
     * General purpose assistant method.
     */
    @Cacheable(value = "groq_ai_keywords", key = "@gptService.generateHash(#prompt)")
    public String ask(String prompt) {
        return executeWithRotation("You are a helpful assistant.", prompt, false);
    }

    // =========================
    // CORE ROTATION WRAPPER
    // =========================
    private String executeWithRotation(String systemMsg, String userMsg, boolean jsonMode) {
        for (int i = 0; i < apiKeys.size(); i++) {
            try {
                return callGroq(systemMsg, userMsg, jsonMode);
            } catch (GroqQuotaException e) {
                rotateKey();
                if (i == apiKeys.size() - 1) {
                    return jsonMode 
                        ? "{\"error\": \"LIMIT_REACHED\", \"message\": \"Service capacity reached.\"}"
                        : "AI service capacity reached.";
                }
            } catch (Exception e) {
                System.err.println("[EVO-ERROR] Key #" + (currentKeyIndex.get() + 1) + " failure: " + e.getMessage());
                if (i < apiKeys.size() - 1) {
                    rotateKey();
                } else {
                    return jsonMode 
                        ? "{\"error\": \"SERVER_ERROR\", \"message\": \"AI failure: " + e.getMessage() + "\"}"
                        : "ERROR: AI service is currently unavailable.";
                }
            }
        }
        return "ERROR: Service unavailable.";
    }

    private String callGroq(String systemMsg, String userMsg, boolean jsonMode) throws Exception {
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL_ID);
        
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemMsg));
        messages.put(new JSONObject().put("role", "user").put("content", userMsg));
        requestBody.put("messages", messages);

        if (jsonMode) {
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + getActiveKey())
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) throw new GroqQuotaException("Rate limit hit");
        
        if (response.statusCode() != 200) {
            System.err.println("[GROQ-DEBUG] Status: " + response.statusCode() + " | Body: " + response.body());
            throw new RuntimeException("HTTP " + response.statusCode());
        }

        return parseGroqResponse(response.body());
    }

    private String parseGroqResponse(String body) {
        try {
            JSONObject json = new JSONObject(body);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content").trim();
        } catch (Exception e) {
            throw new RuntimeException("Parsing error");
        }
    }

    public String generateHash(String input) {
        if (input == null) return "null";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    public static class GroqQuotaException extends RuntimeException {
        public GroqQuotaException(String message) { super(message); }
    }
}