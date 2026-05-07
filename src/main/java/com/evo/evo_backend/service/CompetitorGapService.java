package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.model.CompetitorGapAnalysisRequest;
import com.evo.evo_backend.model.CompetitorGapAnalysisResponse;

@Service
public class CompetitorGapService {

    @Autowired
    private AuditService auditService;

    @Autowired
    private GPTService gptService;

    public CompetitorGapAnalysisResponse analyze(CompetitorGapAnalysisRequest request) {

        if (request.getYourUrl() == null || request.getYourUrl().isBlank()) {
            throw new RuntimeException("Your URL is required");
        }

        if (request.getCompetitorUrls() == null || request.getCompetitorUrls().isEmpty()) {
            throw new RuntimeException("At least one competitor URL is required for gap analysis");
        }

        // 1. Audit Primary Site
        AuditResponse yourAudit = auditService.analyze(request.getYourUrl());
        int yourScore = yourAudit.getScore();

        // 2. Audit Competitors & Calculate Averages
        Map<String, Integer> competitorScores = new HashMap<>();
        int total = 0;
        int successfulAudits = 0;

        for (String url : request.getCompetitorUrls()) {
            try {
                AuditResponse audit = auditService.analyze(url);
                competitorScores.put(url, audit.getScore());
                total += audit.getScore();
                successfulAudits++;
            } catch (Exception e) {
                // Log error and skip failed competitor fetch
                competitorScores.put(url, 0); 
            }
        }

        int avgScore = (successfulAudits > 0) ? (total / successfulAudits) : 0;
        int gap = avgScore - yourScore;

        // 3. Logic-Based Opportunities
        List<String> opportunities = new ArrayList<>();
        if (yourScore < avgScore) {
            opportunities.add("Technical Gap: Close the " + Math.abs(gap) + " point authority difference vs industry leaders");
        }
        if (yourAudit.getContentScore() < 60) {
            opportunities.add("Content Depth: Competitors likely rank higher due to better semantic structure and word count");
        }
        if (yourAudit.getAiVisibilityScore() < 70) {
            opportunities.add("AI Strategy: Optimize for LLM crawlability to appear in AI-generated answers");
        }
        if (yourAudit.getSchemaCount() == 0) {
            opportunities.add("Structured Data: Adding JSON-LD will provide an immediate edge over non-schema competitors");
        }

        // 4. Competitor-Specific Threats
        List<String> threats = new ArrayList<>();
        competitorScores.forEach((url, score) -> {
            if (score > yourScore + 15) {
                threats.add("Major Threat: " + url + " dominates technical SEO with a score of " + score);
            } else if (score > yourScore) {
                threats.add("Moderate Threat: " + url + " has better content optimization than your current version");
            }
        });

        // 5. AI Battle Strategy Summary
        String summary;
        try {
            String prompt = String.format(
                "Compare My Site (%s, Score: %d) with Competitors (Avg: %d). " +
                "Primary Issues: %s. Provide a short, aggressive 2-sentence battle strategy to overtake them.",
                request.getYourUrl(), yourScore, avgScore, yourAudit.getIssues()
            );
            summary = gptService.analyzeContent(prompt);
        } catch (Exception e) {
            summary = "Strategy: Focus on content depth and technical schema implementation to close the industry gap.";
        }

        return new CompetitorGapAnalysisResponse(
                request.getYourUrl(),
                yourScore,
                competitorScores,
                avgScore,
                gap,
                opportunities,
                threats,
                summary
        );
    }
}