package com.evo.evo_backend.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "scans")
public class Scan {

    @Id
    private String id;

    private String url;
    private Scores scores;
    private List<Issue> issues;
    private List<Fix> fixes;
    private String aiSummary;

    // ✅ REQUIRED
    public Scan() {}

    // =========================
    // GETTERS & SETTERS
    // =========================

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Scores getScores() {
        return scores;
    }

    public void setScores(Scores scores) {
        this.scores = scores;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    public List<Fix> getFixes() {
        return fixes;
    }

    public void setFixes(List<Fix> fixes) {
        this.fixes = fixes;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}