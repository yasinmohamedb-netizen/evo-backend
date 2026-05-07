package com.evo.evo_backend.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "audit_history")
public class AuditHistory {
    
    @Id
    private String id;
    private String url;
    private int overallScore;
    private Map<String, Integer> subScores;
    private LocalDateTime timestamp;

    // Constructors
    public AuditHistory() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditHistory(String url, int overallScore, Map<String, Integer> subScores) {
        this.url = url;
        this.overallScore = overallScore;
        this.subScores = subScores;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public Map<String, Integer> getSubScores() { return subScores; }
    public void setSubScores(Map<String, Integer> subScores) { this.subScores = subScores; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}