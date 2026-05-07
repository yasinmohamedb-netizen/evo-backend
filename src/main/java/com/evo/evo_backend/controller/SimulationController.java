package com.evo.evo_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.service.GPTService;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*") 
public class SimulationController {

    private final GPTService gptService;

    @Autowired
    public SimulationController(GPTService gptService) {
        this.gptService = gptService;
    }

    /**
     * 1. AI PERCEPTION SIMULATOR
     * Purpose: Shows how AI currently interprets the brand.
     */
    @PostMapping("/simulate-search")
    public ResponseEntity<?> simulateSearch(@RequestBody Map<String, String> request) {
        String companyName = request.getOrDefault("companyName", "the brand");
        String rawContent = request.getOrDefault("rawContent", "");

        if (rawContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No content provided"));
        }

        String prompt = String.format(
            "Return a JSON object with EXACTLY these keys: 'brand_name', 'description', 'unique_feature', 'benefit', 'reason_to_use', 'pricing', 'location'. " +
            "Analyze company: %s based on text: %s", companyName, rawContent
        );

        try {
            String aiResponse = gptService.getUnifiedAnalysis(prompt);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(aiResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "AI Engine Error"));
        }
    }

    /**
     * 2. SEMANTIC GAP ANALYSIS (THE PROBLEM)
     * Purpose: Identifies specific missing signals to justify optimization.
     */
    @PostMapping("/analyze-gaps")
    public ResponseEntity<?> analyzeGaps(@RequestBody Map<String, String> request) {
        // FIXED: Added variable declaration inside this method scope
        String companyName = request.getOrDefault("companyName", "this business");
        String rawContent = request.get("rawContent");

        if (rawContent == null || rawContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No content provided."));
        }

        String gapPrompt = String.format(
            "Act as a HARSH AI Search Auditor. Analyze this text for %s. " +
            "Identify 3 CRITICAL SEMANTIC GAPS that prevent an AI from recommending this firm " +
            "(e.g., Missing Location/City entities, No specific project scale indicators, lack of unique technical expertise). " +
            "Even if the text is 'good', you MUST find 3 ways to make it more machine-readable. " +
            "Return ONLY a JSON array of objects with keys: 'issue' and 'resolution'. " +
            "TEXT: %s", 
            companyName, 
            rawContent
        );

        try {
            String aiResponse = gptService.getUnifiedAnalysis(gapPrompt);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(aiResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Gap analysis failed."));
        }
    }

    /**
     * 3. SEMANTIC CONTENT TRANSFORMER (THE SOLUTION)
     * Purpose: Converts 'low signal' text into 'high density' AI signals.
     */
    @PostMapping("/optimize-content")
    public ResponseEntity<?> optimizeContent(@RequestBody Map<String, String> request) {
        String content = request.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No content provided."));
        }

        try {
            String optimized = gptService.analyzeContent(content);
            return ResponseEntity.ok(Map.of("optimizedContent", optimized));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Optimization failed."));
        }
    }
}