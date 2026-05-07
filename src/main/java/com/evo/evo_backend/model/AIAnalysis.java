package com.evo.evo_backend.model;

import java.util.List;

public class AIAnalysis {

    private final String summary;
    private final List<String> missing;
    private final List<String> improvements;

    public AIAnalysis(String summary, List<String> missing, List<String> improvements) {
        this.summary = summary;
        this.missing = missing;
        this.improvements = improvements;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getMissing() {
        return missing;
    }

    public List<String> getImprovements() {
        return improvements;
    }
}