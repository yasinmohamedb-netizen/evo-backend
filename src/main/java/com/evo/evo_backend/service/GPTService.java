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

    // ============================================================
    // 🔥 GROQ CONFIG
    // ============================================================

    private static final String MODEL_ID = "llama-3.3-70b-versatile";

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final List<String> apiKeys;

    private final AtomicInteger currentKeyIndex =
            new AtomicInteger(0);

    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

    // ============================================================
    // 🔥 CONSTRUCTOR
    // ============================================================

    public GPTService(
            @Value("${groq.api.key:}") String keys
    ) {

        System.out.println("[EVO-LOG] Initializing GPTService...");

        if (keys == null || keys.isBlank()) {

            System.err.println(
                    "[EVO-ERROR] GROQ_API_KEY missing."
            );

            this.apiKeys = List.of();

            return;
        }

        this.apiKeys = Arrays.stream(keys.split(","))
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .collect(Collectors.toList());

        System.out.println(
                "[EVO-LOG] Loaded "
                        + apiKeys.size()
                        + " Groq API key(s)."
        );
    }

    // ============================================================
    // 🔥 ACTIVE KEY
    // ============================================================

    private String getActiveKey() {

        if (apiKeys == null || apiKeys.isEmpty()) {

            throw new RuntimeException(
                    "No Groq API keys configured."
            );
        }

        return apiKeys.get(
                currentKeyIndex.get() % apiKeys.size()
        );
    }

    // ============================================================
    // 🔥 ROTATE KEY
    // ============================================================

    private void rotateKey() {

        if (apiKeys.size() > 1) {

            int nextIndex =
                    (currentKeyIndex.get() + 1)
                            % apiKeys.size();

            currentKeyIndex.set(nextIndex);

            System.out.println(
                    "[EVO-LOG] Rotated to key index: "
                            + nextIndex
            );
        }
    }

    // ============================================================
    // 🔥 UNIFIED ANALYSIS
    // ============================================================

    @Cacheable(
            value = "groq_unified_analysis",
            key = "@gptService.generateHash(#prompt)"
    )
    public String getUnifiedAnalysis(String prompt) {

        System.out.println(
                "[EVO-LOG] getUnifiedAnalysis triggered."
        );

        String systemMsg =
                "You are a JSON-only response engine. "
                        + "Return valid JSON only.";

        return executeWithRotation(
                systemMsg,
                prompt,
                true
        );
    }

    // ============================================================
    // 🔥 CONTENT ANALYSIS
    // ============================================================

    @Cacheable(
            value = "groq_ai_analysis",
            key = "@gptService.generateHash(#content)"
    )
    public String analyzeContent(String content) {

        if (content == null || content.isBlank()) {
            return "No content provided.";
        }

        System.out.println(
                "[EVO-LOG] analyzeContent triggered."
        );

        String truncated =
                content.length() > 3000
                        ? content.substring(0, 3000)
                        : content;

        String systemMsg =
                "You are an expert AI Semantic SEO Analyst.";

        String prompt =
                "Rewrite the following content "
                        + "to be AI-native and optimized "
                        + "for LLM understanding.\n\n"
                        + truncated;

        return executeWithRotation(
                systemMsg,
                prompt,
                false
        );
    }

    // ============================================================
    // 🔥 GENERAL ASK
    // ============================================================

    @Cacheable(
            value = "groq_ai_keywords",
            key = "@gptService.generateHash(#prompt)"
    )
    public String ask(String prompt) {

        return executeWithRotation(
                "You are a helpful assistant.",
                prompt,
                false
        );
    }

    // ============================================================
    // 🔥 CORE EXECUTION
    // ============================================================

    private String executeWithRotation(
            String systemMsg,
            String userMsg,
            boolean jsonMode
    ) {

        if (apiKeys == null || apiKeys.isEmpty()) {

            return jsonMode
                    ? "{\"error\":\"Missing API Key\"}"
                    : "Missing API Key";
        }

        for (int i = 0; i < apiKeys.size(); i++) {

            try {

                return callGroq(
                        systemMsg,
                        userMsg,
                        jsonMode
                );

            } catch (GroqQuotaException e) {

                rotateKey();

                if (i == apiKeys.size() - 1) {

                    return jsonMode
                            ? "{\"error\":\"LIMIT_REACHED\"}"
                            : "AI service capacity reached.";
                }

            } catch (Exception e) {

                System.err.println(
                        "[EVO-ERROR] "
                                + e.getMessage()
                );

                if (i < apiKeys.size() - 1) {

                    rotateKey();

                } else {

                    return jsonMode
                            ? "{\"error\":\"SERVER_ERROR\"}"
                            : "AI service unavailable.";
                }
            }
        }

        return "Service unavailable.";
    }

    // ============================================================
    // 🔥 GROQ API CALL
    // ============================================================

    private String callGroq(
            String systemMsg,
            String userMsg,
            boolean jsonMode
    ) throws Exception {

        JSONObject requestBody =
                new JSONObject();

        requestBody.put("model", MODEL_ID);

        JSONArray messages =
                new JSONArray();

        messages.put(
                new JSONObject()
                        .put("role", "system")
                        .put("content", systemMsg)
        );

        messages.put(
                new JSONObject()
                        .put("role", "user")
                        .put("content", userMsg)
        );

        requestBody.put("messages", messages);

        if (jsonMode) {

            JSONObject responseFormat =
                    new JSONObject();

            responseFormat.put(
                    "type",
                    "json_object"
            );

            requestBody.put(
                    "response_format",
                    responseFormat
            );
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(GROQ_URL))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + getActiveKey()
                        )
                        .timeout(Duration.ofSeconds(30))
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                requestBody.toString()
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() == 429) {

            throw new GroqQuotaException(
                    "Rate limit hit"
            );
        }

        if (response.statusCode() != 200) {

            System.err.println(
                    "[GROQ-DEBUG] Status: "
                            + response.statusCode()
                            + " | Body: "
                            + response.body()
            );

            throw new RuntimeException(
                    "HTTP " + response.statusCode()
            );
        }

        return parseGroqResponse(response.body());
    }

    // ============================================================
    // 🔥 PARSE RESPONSE
    // ============================================================

    private String parseGroqResponse(String body) {

        try {

            JSONObject json =
                    new JSONObject(body);

            return json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Groq response parsing failed."
            );
        }
    }

    // ============================================================
    // 🔥 HASH GENERATOR
    // ============================================================

    public String generateHash(String input) {

        if (input == null) {
            return "null";
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            input.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getEncoder()
                    .encodeToString(hash);

        } catch (Exception e) {

            return String.valueOf(
                    input.hashCode()
            );
        }
    }

    // ============================================================
    // 🔥 CUSTOM EXCEPTION
    // ============================================================

    public static class GroqQuotaException
            extends RuntimeException {

        public GroqQuotaException(String message) {
            super(message);
        }
    }
}