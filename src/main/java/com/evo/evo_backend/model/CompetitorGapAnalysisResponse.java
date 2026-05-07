package com.evo.evo_backend.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompetitorGapAnalysisResponse {

    private String yourUrl;
    private int yourScore;
    private Map<String, Integer> competitorScores = new HashMap<>();
    private int averageScore;
    private int scoreGap;
    private List<String> opportunities = new ArrayList<>();
    private List<String> threats = new ArrayList<>();
    private String summary;

    public CompetitorGapAnalysisResponse(
            String yourUrl,
            int yourScore,
            Map<String, Integer> competitorScores,
            int averageScore,
            int scoreGap,
            List<String> opportunities,
            List<String> threats,
            String summary
    ) {
        this.yourUrl = yourUrl;
        this.yourScore = yourScore;
        this.competitorScores = competitorScores != null ? competitorScores : new HashMap<>();
        this.averageScore = averageScore;
        this.scoreGap = scoreGap;
        this.opportunities = opportunities != null ? opportunities : new ArrayList<>();
        this.threats = threats != null ? threats : new ArrayList<>();
        this.summary = summary;
    }

    public String getYourUrl() { return yourUrl; }
    public int getYourScore() { return yourScore; }
    public Map<String, Integer> getCompetitorScores() { return competitorScores; }
    public int getAverageScore() { return averageScore; }
    public int getScoreGap() { return scoreGap; }
    public List<String> getOpportunities() { return opportunities; }
    public List<String> getThreats() { return threats; }
    public String getSummary() { return summary; }
}