package com.evo.evo_backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContentGenerationService {

    @Autowired
    private GPTService gptService;

    /**
     * Existing method for generating full SEO pages
     */
    public String generateSEOPage(String businessName, String businessType, List<String> keywords, String location) {
        String keywordList = (keywords != null && !keywords.isEmpty()) 
                             ? String.join(", ", keywords) 
                             : "relevant industry terms";
        
        String prompt = String.format(
            "Act as a world-class SEO strategist. Generate professional HTML for '%s' (Industry: %s) in %s. " +
            "Focus on [%s]. Return only inner HTML.",
            businessName, businessType, location, keywordList
        );

        return gptService.ask(prompt);
    }

    /**
     * 🔥 NEW: AI-Specific Content Transformer
     * Converts "Marketing Fluff" into "High-Density Semantic Signal" for LLMs.
     */
    public String transformForAI(String originalContent) {
        if (originalContent == null || originalContent.isBlank()) {
            return "Please provide content to transform.";
        }

        String prompt = """
            Act as an AI SEO & Semantic Web Expert. 
            Transform the following content into 'LLM-Optimized' text that is easier for AI Search Engines to index.
            
            GUIDELINES:
            1. INCREASE INFORMATION DENSITY: Replace vague adjectives (e.g., 'world-class', 'innovative') with factual data or declarative statements.
            2. SEMANTIC CLARITY: Use clear Entity-Relationship structures (e.g., 'Company X provides Y' instead of 'We do everything').
            3. RAG-FRIENDLY: Structure the response so it is easy for Retrieval-Augmented Generation (RAG) systems to 'chunk' and extract answers.
            4. FORMATTING: Use <strong> for key entities and bullet points for features.
            
            ORIGINAL CONTENT:
            """ + originalContent;

        return gptService.ask(prompt);
    }
}