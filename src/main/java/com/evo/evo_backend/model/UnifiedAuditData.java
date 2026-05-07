package com.evo.evo_backend.model;

import java.util.List;

public class UnifiedAuditData {
    private List<String> keyFindings; 
    private List<KeywordInsight> keywords; 
    private List<String> contentGaps; 
    private Object faqSchema; 

    // Default Constructor
    public UnifiedAuditData() {}

    // Full Constructor
    public UnifiedAuditData(List<String> keyFindings, List<KeywordInsight> keywords, List<String> contentGaps, Object faqSchema) {
        this.keyFindings = keyFindings;
        this.keywords = keywords;
        this.contentGaps = contentGaps;
        this.faqSchema = faqSchema;
    }

    // Getters
    public List<String> getKeyFindings() { return keyFindings; }
    public List<KeywordInsight> getKeywords() { return keywords; }
    public List<String> getContentGaps() { return contentGaps; }
    public Object getFaqSchema() { return faqSchema; }

    // Setters
    public void setKeyFindings(List<String> keyFindings) { this.keyFindings = keyFindings; }
    public void setKeywords(List<KeywordInsight> keywords) { this.keywords = keywords; }
    public void setContentGaps(List<String> contentGaps) { this.contentGaps = contentGaps; }
    public void setFaqSchema(Object faqSchema) { this.faqSchema = faqSchema; }
}