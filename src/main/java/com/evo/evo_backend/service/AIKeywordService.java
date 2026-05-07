package com.evo.evo_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Extracts a primary search keyword from page content using GPT/Gemini.
 *
 * CRITICAL FIX: GPTService.ask() returns raw error strings (e.g. Gemini 429)
 * instead of throwing exceptions. We validate the raw response BEFORE using it.
 * Any response containing error signals is discarded and businessType fallback is used.
 */
@Service
public class AIKeywordService {

    @Autowired
    private GPTService gptService;

    // =========================
    // MAIN ENTRY POINT
    // =========================
    public String extractKeyword(String content, String businessType, String location) {

        if (content != null && content.length() > 100) {
            try {
                String trimmed = content.length() > 1500
                        ? content.substring(0, 1500)
                        : content;

                // gptService.ask() may return a raw error string — treat it like any other response
                String rawResponse = gptService.ask(buildPrompt(trimmed));

                // Validate BEFORE cleaning — raw error strings are long and contain error keywords
                if (rawResponse != null && !containsErrorSignals(rawResponse)) {
                    String keyword = clean(rawResponse);
                    if (isValidKeyword(keyword)) {
                        return keyword;
                    }
                }

                // If we got here, AI returned an error or garbage — use fallback silently
            } catch (Exception ignored) {
                // Exception from gptService — fall through to fallback
            }
        }

        return buildFallback(businessType);
    }

    // =========================
    // ERROR SIGNAL DETECTION
    // Checks the RAW response (before cleaning) for Gemini/OpenAI error patterns
    // This runs on the full string so long error dumps are caught immediately
    // =========================
    private boolean containsErrorSignals(String raw) {
        if (raw == null || raw.length() > 500) return true; // anything over 500 chars is not a keyword

        String lower = raw.toLowerCase();
        return lower.contains("429")              ||
               lower.contains("quota")            ||
               lower.contains("rate limit")       ||
               lower.contains("ratelimit")        ||
               lower.contains("exceeded")         ||
               lower.contains("resourceexhausted")||
               lower.contains("billing")          ||
               lower.contains("retry")            ||
               lower.contains("retrydelay")       ||
               lower.contains("gemini")           ||
               lower.contains("openai")           ||
               lower.contains("generativelanguage")||
               lower.contains("googleapis")       ||
               lower.contains("\"error\"")        ||
               lower.contains("\"code\"")         ||
               lower.contains("\"message\"")      ||
               lower.contains("\"status\"");
    }

    // =========================
    // KEYWORD VALIDATION
    // Runs on the CLEANED response
    // =========================
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.length() < 3 || keyword.length() > 60) return false;

        String lower = keyword.toLowerCase();
        return !lower.equals("business") &&
               !lower.equals("website")  &&
               !lower.equals("company")  &&
               !lower.equals("general")  &&
               !lower.equals("unknown");
    }

    // =========================
    // BUSINESSTYPE → SEARCH KEYWORD
    // =========================
    public String buildFallback(String businessType) {
        if (businessType == null || businessType.isBlank()) return "local business";

        return switch (businessType.toLowerCase()) {
            case "hospital", "healthcare", "clinic", "medical" -> "hospital";
            case "hotel", "accommodation", "resort"            -> "hotel";
            case "school", "education", "university", "college"-> "school";
            case "restaurant", "food", "cafe", "bakery"        -> "restaurant";
            case "retail", "shop", "store", "ecommerce"        -> "retail store";
            case "technology", "software", "saas", "tech"      -> "software company";
            case "law", "legal", "lawyer", "attorney"          -> "law firm";
            case "finance", "bank", "banking", "insurance"     -> "bank";
            case "fitness", "gym", "sports"                    -> "gym";
            case "beauty", "salon", "spa"                      -> "beauty salon";
            case "real estate", "property"                     -> "real estate agency";
            case "media", "news", "blog"                       -> "media company";
            case "travel", "tours", "tourism"                  -> "travel agency";
            default -> businessType.toLowerCase();
        };
    }

    // =========================
    // GPT PROMPT
    // =========================
    private String buildPrompt(String content) {
        return """
You are an SEO expert.

Extract the PRIMARY BUSINESS CATEGORY keyword from this website content.

Rules:
- Return ONLY one short phrase (2-4 words max)
- Must be a generic business category, NOT a brand or company name
- Must be what users type in Google to find this type of business
- No explanations, no punctuation, no quotes, no markdown

Good responses:
hospital
private hospital
food delivery app
online clothing store
law firm

BAD responses (never return these):
Apollo Hospitals
Google
example.com

CONTENT:
""" + content;
    }

    // =========================
    // CLEAN GPT RESPONSE
    // =========================
    private String clean(String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?i)^(keyword[:\\s]*|answer[:\\s]*|phrase[:\\s]*)", "")
                .replaceAll("[\"'`*#\n\r]", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .toLowerCase()
                .trim();
    }
}