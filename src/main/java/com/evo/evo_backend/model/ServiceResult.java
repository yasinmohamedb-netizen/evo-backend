package com.evo.evo_backend.model;

import java.util.List;

public class ServiceResult {

    private String serviceName;
    private int score;
    private List<String> issues;
    private List<String> fixes;
    private String rawContent; // The text used by the AI Schema Generator

    // ✅ 1. Default constructor (Required for JSON mapping)
    public ServiceResult() {
    }

    // ✅ 2. The "Backward Compatible" Constructor
    // This fixes the errors in AccessibilityService, ContentFreshnessService, etc.
    public ServiceResult(String serviceName, int score, List<String> issues, List<String> fixes) {
        this.serviceName = serviceName;
        this.score = score;
        this.issues = issues;
        this.fixes = fixes;
        this.rawContent = ""; // Default to empty string
    }

    // ✅ 3. The "AI-Enabled" Constructor
    // Use this when you want to pass the scraped text to the frontend
    public ServiceResult(String serviceName, int score, List<String> issues, List<String> fixes, String rawContent) {
        this.serviceName = serviceName;
        this.score = score;
        this.issues = issues;
        this.fixes = fixes;
        this.rawContent = rawContent;
    }

    // =========================
    // GETTERS
    // =========================

    public String getServiceName() {
        return serviceName;
    }

    public int getScore() {
        return score;
    }

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getFixes() {
        return fixes;
    }

    public String getRawContent() {
        return rawContent;
    }

    // =========================
    // SETTERS
    // =========================

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public void setFixes(List<String> fixes) {
        this.fixes = fixes;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    // =========================
    // toString (for debugging)
    // =========================

    @Override
    public String toString() {
        return "ServiceResult{" +
                "serviceName='" + serviceName + '\'' +
                ", score=" + score +
                ", rawContentLength=" + (rawContent != null ? rawContent.length() : 0) +
                '}';
    }
}