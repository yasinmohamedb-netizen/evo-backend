package com.evo.evo_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.UnifiedAuditData;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class UnifiedAIService {

    @Autowired
    private GPTService gptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes the Master Prompt to get a comprehensive SEO analysis in one API call.
     */
    public UnifiedAuditData getFullAnalysis(String rawContent, String businessType, String location) {
        // Truncate content to fit within token limits (approx 2500 chars)
        String context = (rawContent != null && rawContent.length() > 2500) 
                         ? rawContent.substring(0, 2500) : rawContent;

        // The Master Prompt - requesting the specific counts you wanted
        String prompt = String.format(
            "Perform a deep SEO audit for a '%s' business located in '%s'.\n" +
            "Return a JSON object with exactly these keys:\n" +
            "1. 'keyFindings': Array of 7 detailed sentences about SEO health.\n" +
            "2. 'keywords': Array of 15 objects (keyword, intent, relevanceScore, difficulty, status).\n" +
            "3. 'contentGaps': Array of 5 specific topics missing from this content.\n" +
            "4. 'faqSchema': A valid JSON-LD FAQPage object based on the content.\n\n" +
            "Content to analyze: %s",
            businessType, location, context
        );

        try {
            // Call the new JSON-mode method in GPTService
            String jsonResponse = gptService.getUnifiedAnalysis(prompt);
            
            // Parse the JSON string into our Java Model
            return objectMapper.readValue(jsonResponse, UnifiedAuditData.class);
            
        } catch (Exception e) {
            System.err.println("[EVO-ERROR] Unified Analysis Failed: " + e.getMessage());
            return getFallbackData();
        }
    }

    private UnifiedAuditData getFallbackData() {
        UnifiedAuditData fallback = new UnifiedAuditData();
        fallback.setKeyFindings(java.util.List.of("AI Analysis temporarily unavailable. Check technical SEO basics."));
        fallback.setKeywords(java.util.List.of());
        fallback.setContentGaps(java.util.List.of("Content depth", "Keyword optimization"));
        return fallback;
    }
}