package com.evo.evo_backend.model;

import java.util.List;

public class SiteMetrics {
    private String url;
    private Integer overallScore;
    private Integer contentScore;
    private Integer technicalScore;
    private Integer backlinksEstimate;
    private Integer keywordCount;
    private Boolean hasSitemap;
    private Boolean hasRobotsTxt;
    private Boolean hasStructuredData;
    private Integer contentLength;
    private List<String> topKeywords;
    private String lastAnalyzedAt;

    // Constructor
    public SiteMetrics() {}

    public SiteMetrics(String url, Integer overallScore, Integer contentScore, 
                       Integer technicalScore, Integer backlinksEstimate, 
                       Integer keywordCount, Boolean hasSitemap, Boolean hasRobotsTxt,
                       Boolean hasStructuredData, Integer contentLength, 
                       List<String> topKeywords, String lastAnalyzedAt) {
        this.url = url;
        this.overallScore = overallScore;
        this.contentScore = contentScore;
        this.technicalScore = technicalScore;
        this.backlinksEstimate = backlinksEstimate;
        this.keywordCount = keywordCount;
        this.hasSitemap = hasSitemap;
        this.hasRobotsTxt = hasRobotsTxt;
        this.hasStructuredData = hasStructuredData;
        this.contentLength = contentLength;
        this.topKeywords = topKeywords;
        this.lastAnalyzedAt = lastAnalyzedAt;
    }

    // Getters
    public String getUrl()                    { return url; }
    public Integer getOverallScore()          { return overallScore; }
    public Integer getContentScore()          { return contentScore; }
    public Integer getTechnicalScore()        { return technicalScore; }
    public Integer getBacklinksEstimate()     { return backlinksEstimate; }
    public Integer getKeywordCount()          { return keywordCount; }
    public Boolean getHasSitemap()            { return hasSitemap; }
    public Boolean getHasRobotsTxt()          { return hasRobotsTxt; }
    public Boolean getHasStructuredData()     { return hasStructuredData; }
    public Integer getContentLength()         { return contentLength; }
    public List<String> getTopKeywords()      { return topKeywords; }
    public String getLastAnalyzedAt()         { return lastAnalyzedAt; }

    // Setters
    public void setUrl(String url)                          { this.url = url; }
    public void setOverallScore(Integer overallScore)       { this.overallScore = overallScore; }
    public void setContentScore(Integer contentScore)       { this.contentScore = contentScore; }
    public void setTechnicalScore(Integer technicalScore)   { this.technicalScore = technicalScore; }
    public void setBacklinksEstimate(Integer backlinksEstimate) { this.backlinksEstimate = backlinksEstimate; }
    public void setKeywordCount(Integer keywordCount)       { this.keywordCount = keywordCount; }
    public void setHasSitemap(Boolean hasSitemap)          { this.hasSitemap = hasSitemap; }
    public void setHasRobotsTxt(Boolean hasRobotsTxt)      { this.hasRobotsTxt = hasRobotsTxt; }
    public void setHasStructuredData(Boolean hasStructuredData) { this.hasStructuredData = hasStructuredData; }
    public void setContentLength(Integer contentLength)     { this.contentLength = contentLength; }
    public void setTopKeywords(List<String> topKeywords)    { this.topKeywords = topKeywords; }
    public void setLastAnalyzedAt(String lastAnalyzedAt)    { this.lastAnalyzedAt = lastAnalyzedAt; }
}