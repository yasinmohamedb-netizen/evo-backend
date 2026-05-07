package com.evo.evo_backend.model;

public class Fix {

    private String text;
    private String priority;

    // ✅ REQUIRED
    public Fix() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}