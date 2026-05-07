package com.evo.evo_backend.model;

import java.util.ArrayList;
import java.util.List;

public class CompetitorGapAnalysisRequest {

    private String yourUrl;
    private List<String> competitorUrls = new ArrayList<>();

    public CompetitorGapAnalysisRequest() {}

    public String getYourUrl() {
        return yourUrl;
    }

    public void setYourUrl(String yourUrl) {
        this.yourUrl = yourUrl;
    }

    public List<String> getCompetitorUrls() {
        return competitorUrls;
    }

    public void setCompetitorUrls(List<String> competitorUrls) {
        this.competitorUrls = competitorUrls != null ? competitorUrls : new ArrayList<>();
    }
}