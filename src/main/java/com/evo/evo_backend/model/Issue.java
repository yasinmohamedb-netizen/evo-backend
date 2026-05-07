package com.evo.evo_backend.model;

public class Issue {

    private String text;
    private String impact;

    // ✅ REQUIRED
    public Issue() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }
}