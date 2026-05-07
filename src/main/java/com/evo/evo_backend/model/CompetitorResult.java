package com.evo.evo_backend.model;

import java.util.ArrayList;
import java.util.List;

public class CompetitorResult {
    private AuditResponse primary;
    private AuditResponse competitor;
    private List<String> primaryWins;
    private List<String> competitorWins;
    private List<String> recommendations;

    public CompetitorResult(AuditResponse primary, AuditResponse competitor,
                            List<String> primaryWins, List<String> competitorWins, 
                            List<String> recommendations) {
        this.primary = primary;
        this.competitor = competitor;
        this.primaryWins = primaryWins != null ? primaryWins : new ArrayList<>();
        this.competitorWins = competitorWins != null ? competitorWins : new ArrayList<>();
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    // Getters
    public AuditResponse getPrimary() { return primary; }
    public AuditResponse getCompetitor() { return competitor; }
    public List<String> getPrimaryWins() { return primaryWins; }
    public List<String> getCompetitorWins() { return competitorWins; }
    public List<String> getRecommendations() { return recommendations; }
}