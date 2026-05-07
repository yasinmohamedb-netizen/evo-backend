package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class SchemaMarkupService {

    public ServiceResult analyze(Document doc) {
        List<String> issues = new ArrayList<>();
        List<String> fixes = new ArrayList<>();
        List<String> schemaTypes = new ArrayList<>();
        
        int score = 100;

        // 1. Detect existing JSON-LD
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                String json = script.html().trim();
                if (json.startsWith("[")) {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        String type = arr.getJSONObject(i).optString("@type", "");
                        if (!type.isEmpty()) schemaTypes.add(type);
                    }
                } else {
                    String type = new JSONObject(json).optString("@type", "");
                    if (!type.isEmpty()) schemaTypes.add(type);
                }
            } catch (Exception ignored) {}
        }

        // 2. Logic-based Scoring & Prescription
        boolean hasOrg = schemaTypes.stream().anyMatch(t -> 
            t.contains("Organization") || t.contains("LocalBusiness") || t.contains("MedicalBusiness"));
        
        boolean hasFaq = schemaTypes.contains("FAQPage");

        if (schemaTypes.isEmpty()) {
            score = 20; // Critical penalty
            issues.add("No structured data (JSON-LD) detected");
            fixes.add("CRITICAL: Generate and add Organization Schema to verify your business identity with AI models.");
        } else {
            if (!hasOrg) {
                score -= 40;
                issues.add("Missing Organization/Business identity schema");
                fixes.add("Add Organization JSON-LD to help AI verify your brand name and location.");
            }
            if (!hasFaq) {
                score -= 20;
                issues.add("Missing FAQPage schema");
                fixes.add("Add FAQ schema to increase the chances of your content appearing in AI 'snippets'.");
            }
        }

        // 3. Proactive "Better" Logic: Suggesting the fix code
        if (!hasOrg) {
            String suggestedSchema = generatePlaceholderSchema(doc);
            fixes.add("SUGGESTED CODE: " + suggestedSchema);
        }

        score = Math.max(0, Math.min(score, 100));
        return new ServiceResult("Schema Markup", score, issues, fixes);
    }

    /**
     * Better Logic: Instead of just saying it's missing, 
     * we draft the fix for them using data we found on the page.
     */
    private String generatePlaceholderSchema(Document doc) {
        String name = doc.title().split("[|\\-–]")[0].trim();
        String url = doc.baseUri();
        
        JSONObject json = new JSONObject();
        json.put("@context", "https://schema.org");
        json.put("@type", "Organization");
        json.put("name", name);
        json.put("url", url);
        json.put("description", "Update this with a 150-character description of your business.");
        
        return json.toString(); // The frontend can then format this as a code block
    }
}