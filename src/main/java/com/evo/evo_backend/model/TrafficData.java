package com.evo.evo_backend.model;

import java.util.List;

public class TrafficData {
    private String total;
    private String aiReferrals;
    private String growth;
    private List<TrafficSource> sources;

    public TrafficData() {}

    public TrafficData(String total, String aiReferrals, String growth, List<TrafficSource> sources) {
        this.total = total;
        this.aiReferrals = aiReferrals;
        this.growth = growth;
        this.sources = sources;
    }

    // Getters & Setters
    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }

    public String getAiReferrals() { return aiReferrals; }
    public void setAiReferrals(String aiReferrals) { this.aiReferrals = aiReferrals; }

    public String getGrowth() { return growth; }
    public void setGrowth(String growth) { this.growth = growth; }

    public List<TrafficSource> getSources() { return sources; }
    public void setSources(List<TrafficSource> sources) { this.sources = sources; }
}