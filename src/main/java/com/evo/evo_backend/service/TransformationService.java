package com.evo.evo_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransformationService {

    @Autowired
    private GPTService gptService; // Now resolved via Autowired

    /**
     * Analyzes the gap between primary and competitor content 
     * and generates bridge content.
     */
    public String generateContentBridge(String myContent, String competitorContent) {
        if (myContent == null || competitorContent == null) {
            return "Insufficient content to generate a bridge.";
        }

        String prompt = "Compare my content with the competitor's content. " +
                        "Identify 3 high-impact topics they cover that I am missing. " +
                        "Write 3 professional paragraphs (150 words each) for my website to bridge this gap " +
                        "while maintaining a unique brand voice.\n\n" +
                        "MY CONTENT:\n" + myContent + "\n\n" +
                        "COMPETITOR CONTENT:\n" + competitorContent;

        // Using your existing GPTService logic
        return gptService.getUnifiedAnalysis(prompt); 
    }
}