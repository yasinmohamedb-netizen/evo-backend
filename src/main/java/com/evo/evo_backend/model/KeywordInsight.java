package com.evo.evo_backend.model;

public class KeywordInsight {
    private String keyword;
    private String intent;      // Informational, Transactional, etc.
    private int relevanceScore; // 0-100
    private String difficulty;  // Low, Medium, High
    private String status;      // "Present" or "Missing"

    // Default Constructor
    public KeywordInsight() {}

    // Full Constructor
    public KeywordInsight(String keyword, String intent, int relevanceScore, String difficulty, String status) {
        this.keyword = keyword;
        this.intent = intent;
        this.relevanceScore = relevanceScore;
        this.difficulty = difficulty;
        this.status = status;
    }

    // Getters
    public String getKeyword() { return keyword; }
    public String getIntent() { return intent; }
    public int getRelevanceScore() { return relevanceScore; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }

    // Setters
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public void setIntent(String intent) { this.intent = intent; }
    public void setRelevanceScore(int relevanceScore) { this.relevanceScore = relevanceScore; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setStatus(String status) { this.status = status; }
}