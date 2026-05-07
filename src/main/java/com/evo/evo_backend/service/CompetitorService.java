package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.model.CompetitorResult;

@Service
public class CompetitorService {

    @Autowired
    private AuditService auditService;

    public CompetitorResult compare(String primaryUrl, String competitorUrl) {

        // 1. Run full audits for both sites
        AuditResponse primary    = auditService.analyze(primaryUrl);
        AuditResponse competitor = auditService.analyze(competitorUrl);

        List<String> primaryWins     = new ArrayList<>();
        List<String> competitorWins  = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        String pName = primary.getCompanyName();
        String cName = competitor.getCompanyName();

        // 2. Core Score Comparison
        compareScore("Overall AI Visibility", pName, cName,
                primary.getScore(), competitor.getScore(),
                primaryWins, competitorWins, recommendations,
                "Address technical SEO issues mentioned in your audit report");

        compareScore("Content Quality", pName, cName,
                primary.getContentScore(), competitor.getContentScore(),
                primaryWins, competitorWins, recommendations,
                "Increase word count, add FAQ sections, and use more headings");

        compareScore("Performance & Speed", pName, cName,
                primary.getPerformanceScore(), competitor.getPerformanceScore(),
                primaryWins, competitorWins, recommendations,
                "Optimise images and reduce render-blocking JavaScript");

        compareScore("AI Bot Crawlability", pName, cName,
                primary.getLlmCrawlabilityScore(), competitor.getLlmCrawlabilityScore(),
                primaryWins, competitorWins, recommendations,
                "Update robots.txt to explicitly allow AI agents and improve schema");

        // 3. Exact Difference Engine (Hard Data Comparison)
        if (primary.getWordCount() < competitor.getWordCount()) {
            recommendations.add("Content Volume: " + cName + " has " + (competitor.getWordCount() - primary.getWordCount()) + " more words than you.");
        }
        if (primary.getSchemaCount() < competitor.getSchemaCount()) {
            recommendations.add("Structured Data: " + cName + " uses " + competitor.getSchemaCount() + " schema blocks; you only use " + primary.getSchemaCount());
        }
        if (competitor.getFaqCount() > 0 && primary.getFaqCount() == 0) {
            recommendations.add("User Experience: Competitor uses FAQs for AI snippets—you are missing this opportunity.");
        }

        // 4. Potential Uplift Logic
        int pUplift = primary.getPotentialScore() - primary.getScore();
        if (pUplift > 20) {
            recommendations.add("High Growth Alert: You have +" + pUplift + " points of untapped potential compared to " + cName);
        }

        return new CompetitorResult(primary, competitor, primaryWins, competitorWins, recommendations);
    }

    private void compareScore(String dimension, String pName, String cName, int pScore, int cScore,
                              List<String> primaryWins, List<String> competitorWins, 
                              List<String> recommendations, String fixHint) {
        if (pScore > cScore + 5) {
            primaryWins.add(pName + " leads on " + dimension + " (" + pScore + " vs " + cScore + ")");
        } else if (cScore > pScore + 5) {
            competitorWins.add(cName + " leads on " + dimension + " (" + cScore + " vs " + pScore + ")");
            recommendations.add(dimension + ": " + fixHint + " to close the " + (cScore - pScore) + "pt gap.");
        }
    }
}