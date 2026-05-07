package com.evo.evo_backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.ServiceResult;
import com.evo.evo_backend.service.LLMCrawlabilityService;
import com.evo.evo_backend.service.SchemaMarkupService;

/**
 * Specialized controller for the "AI Health Check" dashboard.
 * Focuses on real-time crawlability, schema validation, and semantic density.
 */
@RestController
@RequestMapping("/api/health-check")
@CrossOrigin(origins = "*") 
public class HealthCheckController {

    @Autowired 
    private LLMCrawlabilityService crawlabilityService;

    @Autowired 
    private SchemaMarkupService schemaService;

    /**
     * Executes a fast, multi-point audit for the Result page dashboard.
     */
    @GetMapping("/full")
    public Map<String, Object> runFullAudit(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Ensure URL has protocol
            String targetUrl = url;
            if (!targetUrl.startsWith("http")) {
                targetUrl = "https://" + targetUrl;
            }

            // 1. Fetch the document once (Performance optimization)
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) EVO-AI-Bot/1.0")
                    .timeout(10000)
                    .get();

            // 2. Run existing Crawlability & Schema services
            ServiceResult crawlResult = crawlabilityService.analyze(targetUrl);
            ServiceResult schemaResult = schemaService.analyze(doc);
            
            // 3. Run Semantic Density Logic
            int semanticScore = calculateSemanticDensity(doc);

            // 4. Calculate weighted Overall Score
            int overallScore = (crawlResult.getScore() + schemaResult.getScore() + semanticScore) / 3;

            // 5. Build Unified JSON Response
            response.put("url", targetUrl);
            response.put("overallScore", overallScore);
            response.put("crawlability", crawlResult);
            response.put("schema", schemaResult);
            
            response.put("semantic", Map.of(
                "score", semanticScore,
                "foundWho", doc.text().toLowerCase().contains("we are") || 
                            doc.text().toLowerCase().contains("founded"),
                "foundWhat", doc.select("h1, h2").text().length() > 20,
                "hasDescription", !doc.select("meta[name=description]").isEmpty()
            ));

        } catch (Exception e) {
            response.put("error", "Failed to reach " + url + ". Error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Measures how well the page structure communicates its purpose to AI.
     */
    private int calculateSemanticDensity(Document doc) {
        int score = 40; // Base score for having a reachable page
        
        // +20 for Meta Description (Key for AI Summaries)
        if (!doc.select("meta[name=description]").isEmpty()) score += 20;
        
        // +20 for clear H1 Tag (Subject identification)
        if (!doc.select("h1").isEmpty()) score += 20;
        
        // +20 for Sub-headings (Topic depth)
        if (doc.select("h2").size() >= 2) score += 20;
        
        return score;
    }
}