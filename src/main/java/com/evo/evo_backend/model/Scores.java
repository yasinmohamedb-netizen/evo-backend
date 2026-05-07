package com.evo.evo_backend.model;

public class Scores {

    private int aiVisibility;
    private int content;
    private int schema;
    private int eeat;

    // ✅ REQUIRED
    public Scores() {}

    public int getAiVisibility() {
        return aiVisibility;
    }

    public void setAiVisibility(int aiVisibility) {
        this.aiVisibility = aiVisibility;
    }

    public int getContent() {
        return content;
    }

    public void setContent(int content) {
        this.content = content;
    }

    public int getSchema() {
        return schema;
    }

    public void setSchema(int schema) {
        this.schema = schema;
    }

    public int getEeat() {
        return eeat;
    }

    public void setEeat(int eeat) {
        this.eeat = eeat;
    }
}