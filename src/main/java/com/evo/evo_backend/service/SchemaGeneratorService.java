package com.evo.evo_backend.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchemaGeneratorService {

    @Autowired
    private GPTService gptService;

    /**
     * Generates FAQ Schema using AI.
     * Includes a cleaning step to remove markdown backticks.
     */
    public String generateFaqSchema(String pageText) {
        if (pageText == null || pageText.trim().isEmpty()) {
            return "{\"error\": \"No content provided to analyze.\"}";
        }

        String prompt = "Act as an SEO expert. Extract 3-5 frequently asked questions and answers from the following text. " +
                        "Return ONLY the valid JSON-LD FAQPage schema. No preamble, no backticks, no explanation.\n\n" +
                        "TEXT:\n" + (pageText.length() > 3500 ? pageText.substring(0, 3500) : pageText);

        try {
            System.out.println("[EVO-LOG] Requesting FAQ Schema from Gemini...");
            String rawResponse = gptService.ask(prompt);
            
            // CLEANING STEP: AI often wraps JSON in ```json blocks which breaks the parser
            String cleanJson = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();
                
            return cleanJson;
        } catch (Exception e) {
            System.err.println("[EVO-ERROR] AI Generation failed: " + e.getMessage());
            return "{\"error\": \"AI could not generate schema at this time.\"}";
        }
    }

    /**
     * Generates Organization Schema instantly using local logic.
     */
    public String generateOrgSchema(String name, String url, String type, String location) {
        try {
            JSONObject schema = new JSONObject();
            schema.put("@context", "https://schema.org");
            
            // Default to Organization if type is null/empty
            String businessType = (type == null || type.isEmpty() || type.equals("General")) 
                                  ? "Organization" : type;
            schema.put("@type", businessType);
            
            schema.put("name", name != null ? name : "My Business");
            schema.put("url", url != null ? url : "");
            
            if (location != null && !location.equalsIgnoreCase("Unknown") && !location.isEmpty()) {
                JSONObject address = new JSONObject();
                address.put("@type", "PostalAddress");
                address.put("addressLocality", location);
                schema.put("address", address);
            }
            
            return schema.toString(4); // Pretty print for the code block
        } catch (Exception e) {
            return "{\"error\": \"Failed to build Organization schema.\"}";
        }
    }
}