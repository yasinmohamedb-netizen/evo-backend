package com.evo.evo_backend.model;

import java.util.List;
import java.util.Map;

public class GapAnalysis {
    private Map<String, Integer> scoreGaps;
    private Map<String, Boolean> featureGaps;
    private Integer averageCompetitorScore;
    private Integer yourScore;
    private Integer scoreDeficit;
    private Integer scoreSurplus;
    private List<String> criticalGaps;

    // Constructor
    public GapAnalysis() {}

    public GapAnalysis(Map<String, Integer> scoreGaps, Map<String, Boolean> featureGaps,
                       Integer averageCompetitorScore, Integer yourScore, 
                       Integer scoreDeficit, Integer scoreSurplus, List<String> criticalGaps) {
        this.scoreGaps = scoreGaps;
        this.featureGaps = featureGaps;
        this.averageCompetitorScore = averageCompetitorScore;
        this.yourScore = yourScore;
        this.scoreDeficit = scoreDeficit;
        this.scoreSurplus = scoreSurplus;
        this.criticalGaps = criticalGaps;
    }

    // Getters
    public Map<String, Integer> getScoreGaps()              { return scoreGaps; }
    public Map<String, Boolean> getFeatureGaps()           { return featureGaps; }
    public Integer getAverageCompetitorScore()             { return averageCompetitorScore; }
    public Integer getYourScore()                          { return yourScore; }
    public Integer getScoreDeficit()                       { return scoreDeficit; }
    public Integer getScoreSurplus()                       { return scoreSurplus; }
    public List<String> getCriticalGaps()                  { return criticalGaps; }

    // Setters
    public void setScoreGaps(Map<String, Integer> scoreGaps)           { this.scoreGaps = scoreGaps; }
    public void setFeatureGaps(Map<String, Boolean> featureGaps)       { this.featureGaps = featureGaps; }
    public void setAverageCompetitorScore(Integer avg)                 { this.averageCompetitorScore = avg; }
    public void setYourScore(Integer yourScore)                        { this.yourScore = yourScore; }
    public void setScoreDeficit(Integer scoreDeficit)                  { this.scoreDeficit = scoreDeficit; }
    public void setScoreSurplus(Integer scoreSurplus)                  { this.scoreSurplus = scoreSurplus; }
    public void setCriticalGaps(List<String> criticalGaps)             { this.criticalGaps = criticalGaps; }
}