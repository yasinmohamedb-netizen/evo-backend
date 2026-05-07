package com.evo.evo_backend.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditResponse {

    private String companyName;

    // =========================
    // OVERALL METRICS
    // =========================
    private int score;
    private int potentialScore;
    private int improvement;
    private String visibility;

    // =========================
    // AI SPECIFIC SCORES
    // =========================
    private int aiSeoScore;
    private int aiVisibilityScore;

    // =========================
    // TECHNICAL SUB-SCORES
    // =========================
    private int contentScore;
    private int performanceScore;
    private int accessibilityScore;
    private int sitemapScore;
    private int socialMetaScore;
    private int linkHealthScore;
    private int llmCrawlabilityScore;

    // =========================
    // RAW DATA (GAP ENGINE)
    // =========================
    private int wordCount;
    private int faqCount;
    private int schemaCount;
    private int headingCount;
    private int internalLinks;
    
    private String rawContent;

    // =========================
    // BUSINESS INTELLIGENCE
    // =========================
    private String location;
    private String businessType;
    private List<Competitor> competitors = new ArrayList<>();
    private List<String> improvements = new ArrayList<>();
    private List<KeywordInsight> keywordInsights = new ArrayList<>();

    // =========================
    // ISSUES + FIXES
    // =========================
    private List<String> issues = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    // =========================
    // AI SUMMARY OUTPUT
    // =========================
    private AIAnalysis aiAnalysis;

    // =========================
    // TRAFFIC ANALYSIS (NEW)
    // =========================
    private TrafficData traffic;

    // =========================
    // CONSTRUCTORS
    // =========================
    public AuditResponse() {
    }

    public AuditResponse(
            String companyName,
            int score,
            int potentialScore,
            int improvement,
            String visibility,
            int aiSeoScore,
            int aiVisibilityScore,
            int contentScore,
            int performanceScore,
            int accessibilityScore,
            int sitemapScore,
            int socialMetaScore,
            int linkHealthScore,
            int llmCrawlabilityScore,
            List<String> issues,
            List<String> suggestions,
            AIAnalysis aiAnalysis,
            String rawContent,
            TrafficData traffic
    ) {
        this.companyName          = companyName;
        this.score                = score;
        this.potentialScore       = potentialScore;
        this.improvement          = improvement;
        this.visibility           = visibility;
        this.aiSeoScore           = aiSeoScore;
        this.aiVisibilityScore    = aiVisibilityScore;
        this.contentScore         = contentScore;
        this.performanceScore     = performanceScore;
        this.accessibilityScore   = accessibilityScore;
        this.sitemapScore         = sitemapScore;
        this.socialMetaScore      = socialMetaScore;
        this.linkHealthScore      = linkHealthScore;
        this.llmCrawlabilityScore = llmCrawlabilityScore;
        this.issues               = issues != null ? issues : new ArrayList<>();
        this.suggestions          = suggestions != null ? suggestions : new ArrayList<>();
        this.aiAnalysis           = aiAnalysis;
        this.rawContent           = rawContent;
        this.traffic              = traffic;
    }

    // =========================
    // GETTERS & SETTERS
    // =========================
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getPotentialScore() { return potentialScore; }
    public void setPotentialScore(int potentialScore) { this.potentialScore = potentialScore; }

    public int getImprovement() { return improvement; }
    public void setImprovement(int improvement) { this.improvement = improvement; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public int getAiSeoScore() { return aiSeoScore; }
    public void setAiSeoScore(int aiSeoScore) { this.aiSeoScore = aiSeoScore; }

    public int getAiVisibilityScore() { return aiVisibilityScore; }
    public void setAiVisibilityScore(int aiVisibilityScore) { this.aiVisibilityScore = aiVisibilityScore; }

    public int getContentScore() { return contentScore; }
    public void setContentScore(int contentScore) { this.contentScore = contentScore; }

    public int getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(int performanceScore) { this.performanceScore = performanceScore; }

    public int getAccessibilityScore() { return accessibilityScore; }
    public void setAccessibilityScore(int accessibilityScore) { this.accessibilityScore = accessibilityScore; }

    public int getSitemapScore() { return sitemapScore; }
    public void setSitemapScore(int sitemapScore) { this.sitemapScore = sitemapScore; }

    public int getSocialMetaScore() { return socialMetaScore; }
    public void setSocialMetaScore(int socialMetaScore) { this.socialMetaScore = socialMetaScore; }

    public int getLinkHealthScore() { return linkHealthScore; }
    public void setLinkHealthScore(int linkHealthScore) { this.linkHealthScore = linkHealthScore; }

    public int getLlmCrawlabilityScore() { return llmCrawlabilityScore; }
    public void setLlmCrawlabilityScore(int llmCrawlabilityScore) { this.llmCrawlabilityScore = llmCrawlabilityScore; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public int getFaqCount() { return faqCount; }
    public void setFaqCount(int faqCount) { this.faqCount = faqCount; }

    public int getSchemaCount() { return schemaCount; }
    public void setSchemaCount(int schemaCount) { this.schemaCount = schemaCount; }

    public int getHeadingCount() { return headingCount; }
    public void setHeadingCount(int headingCount) { this.headingCount = headingCount; }

    public int getInternalLinks() { return internalLinks; }
    public void setInternalLinks(int internalLinks) { this.internalLinks = internalLinks; }

    @JsonProperty("rawText")
    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public List<Competitor> getCompetitors() { return competitors; }
    public void setCompetitors(List<Competitor> competitors) { this.competitors = competitors; }

    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }

    public List<KeywordInsight> getKeywordInsights() { return keywordInsights; }
    public void setKeywordInsights(List<KeywordInsight> keywordInsights) { this.keywordInsights = keywordInsights; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public AIAnalysis getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(AIAnalysis aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public TrafficData getTraffic() { return traffic; }
    public void setTraffic(TrafficData traffic) { this.traffic = traffic; }
}