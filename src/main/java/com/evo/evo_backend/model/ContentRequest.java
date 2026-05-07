package com.evo.evo_backend.model;

import java.util.List;

public class ContentRequest {
    private String businessName;
    private String pageType;
    private List<String> targetKeywords;
    private String location;

    // Getters
    public String getBusinessName() {
        return businessName;
    }

    public String getPageType() {
        return pageType;
    }

    public List<String> getTargetKeywords() {
        return targetKeywords;
    }

    public String getLocation() {
        return location;
    }

    // Setters (Needed for Jackson to map the JSON request body)
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public void setTargetKeywords(List<String> targetKeywords) {
        this.targetKeywords = targetKeywords;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}