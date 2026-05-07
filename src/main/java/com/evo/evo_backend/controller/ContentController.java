package com.evo.evo_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.ContentRequest;
import com.evo.evo_backend.service.GPTService;

@RestController
@RequestMapping("/api/content")
@CrossOrigin(origins = "http://localhost:3000")
public class ContentController {

    // Injecting the updated Groq-powered service directly
    @Autowired
    @Qualifier("gptService")
    private GPTService gptService;

    /**
     * Standard SEO Page Generation
     * Now using a single Unified Groq call to get both HTML and Schema.
     */
    @PostMapping("/generate-page")
    public ResponseEntity<?> generatePage(@RequestBody ContentRequest request) {
        System.out.println("[EVO-LOG] Unified Generation Triggered for: " + request.getBusinessName());

        // Unified Prompt to get valid JSON back from Groq
        String prompt = String.format(
            "Act as an SEO expert. Generate high-converting SEO content and FAQ schema for '%s' located in '%s'. " +
            "The page type is '%s' targeting keywords: %s. " +
            "Return ONLY a JSON object with two keys: " +
            "'html' (the SEO landing page content with <h2> and <p> tags) and " +
            "'schema' (a valid JSON-LD FAQPage object).",
            request.getBusinessName(),
            request.getLocation(),
            request.getPageType(),
            String.join(", ", request.getTargetKeywords())
        );

        try {
            // Get the JSON response from Groq
            String aiResult = gptService.getUnifiedAnalysis(prompt);
            
            // We wrap it in a 'result' key so the frontend SchemaGenerator.jsx can parse it
            return ResponseEntity.ok(Map.of("result", aiResult));
        } catch (Exception e) {
            System.err.println("[EVO-ERROR] Generation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "AI service unavailable: " + e.getMessage()));
        }
    }

    /**
     * AI-Specific Content Transformer
     */
    @PostMapping("/transform")
    public ResponseEntity<Map<String, String>> transform(@RequestBody Map<String, String> request) {
        String originalContent = request.get("content");
        
        if (originalContent == null || originalContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }

        try {
            String transformedContent = gptService.analyzeContent(originalContent);
            return ResponseEntity.ok(Map.of("transformedContent", transformedContent));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Transformation failed"));
        }
    }
}