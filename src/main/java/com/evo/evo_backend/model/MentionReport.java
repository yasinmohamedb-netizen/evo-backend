package com.evo.evo_backend.model;

import java.time.LocalDateTime;
import java.util.List;

public class MentionReport {

    private String domain;
    private String brandName;
    private LocalDateTime checkedAt;

    // Overall result
    private boolean mentioned;
    private int citationScore;          // 0-100
    private String citationLevel;       // NONE / LOW / MEDIUM / HIGH

    // Per query results
    private List<QueryResult> queryResults;

    // Competitors cited instead
    private List<String> competitorsMentioned;

    // Actionable advice
    private List<String> recommendations;

    // =========================
    // ✅ CONSTRUCTOR
    // =========================
    public MentionReport(
            String domain,
            String brandName,
            boolean mentioned,
            int citationScore,
            String citationLevel,
            List<QueryResult> queryResults,
            List<String> competitorsMentioned,
            List<String> recommendations
    ) {
        this.domain               = domain;
        this.brandName            = brandName;
        this.checkedAt            = LocalDateTime.now();
        this.mentioned            = mentioned;
        this.citationScore        = citationScore;
        this.citationLevel        = citationLevel;
        this.queryResults         = queryResults;
        this.competitorsMentioned = competitorsMentioned;
        this.recommendations      = recommendations;
    }

    // =========================
    // ✅ GETTERS
    // =========================
    public String getDomain()                        { return domain; }
    public String getBrandName()                     { return brandName; }
    public LocalDateTime getCheckedAt()              { return checkedAt; }
    public boolean isMentioned()                     { return mentioned; }
    public int getCitationScore()                    { return citationScore; }
    public String getCitationLevel()                 { return citationLevel; }
    public List<QueryResult> getQueryResults()       { return queryResults; }
    public List<String> getCompetitorsMentioned()    { return competitorsMentioned; }
    public List<String> getRecommendations()         { return recommendations; }

    // =========================
    // 📦 INNER CLASS - QueryResult
    // =========================
    public static class QueryResult {

        private String query;
        private boolean brandMentioned;
        private String aiResponse;
        private List<String> mentionedBrands;

        public QueryResult(
                String query,
                boolean brandMentioned,
                String aiResponse,
                List<String> mentionedBrands
        ) {
            this.query           = query;
            this.brandMentioned  = brandMentioned;
            this.aiResponse      = aiResponse;
            this.mentionedBrands = mentionedBrands;
        }

        public String getQuery()                  { return query; }
        public boolean isBrandMentioned()         { return brandMentioned; }
        public String getAiResponse()             { return aiResponse; }
        public List<String> getMentionedBrands()  { return mentionedBrands; }
    }
}