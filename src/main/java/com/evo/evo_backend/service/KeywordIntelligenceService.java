package com.evo.evo_backend.service;

import com.evo.evo_backend.model.KeywordInsight;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class KeywordIntelligenceService {

    @Autowired
    private GPTService gptService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<KeywordInsight> analyzeKeywords(String rawContent, String businessType) {
        String context = (rawContent != null && rawContent.length() > 3000) 
                         ? rawContent.substring(0, 3000) : rawContent;

        String prompt = "Analyze this content for a " + businessType + " business. " +
            "Return a JSON array of the top 5 high-value keywords with these keys: " +
            "\"keyword\", \"intent\", \"relevanceScore\" (int), \"difficulty\", \"status\" (\"Present\" or \"Missing\"). " +
            "Only return the JSON array, no preamble.";

        try {
            String aiResponse = gptService.ask(prompt + "\n\nContent: " + context);
            
            // 🔥 CLEAN THE STRING: Remove markdown backticks if Gemini adds them
            String jsonPart = aiResponse.substring(aiResponse.indexOf("["), aiResponse.lastIndexOf("]") + 1);
            
            return objectMapper.readValue(jsonPart, new TypeReference<List<KeywordInsight>>() {});
        } catch (Exception e) {
            System.err.println("Keyword Parsing Error: " + e.getMessage());
            return getFallbackKeywords(businessType);
        }
    }

    private List<KeywordInsight> getFallbackKeywords(String type) {
        List<KeywordInsight> fallbacks = new ArrayList<>();
        fallbacks.add(new KeywordInsight("SEO Audit Tool", "Commercial", 95, "Medium", "Present"));
        return fallbacks;
    }
}