package com.evo.evo_backend.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.TrafficData;
import com.evo.evo_backend.model.TrafficSource;

@Service
public class TrafficService {

    public TrafficData getLiveTrafficAnalysis(String url) {
        // In production, fetch these from your DB filtered by 'url'
        List<TrafficSource> sources = Arrays.asList(
            new TrafficSource("Perplexity AI", 45, "#3b82f6"),
            new TrafficSource("ChatGPT Search", 30, "#10b981"),
            new TrafficSource("Google Gemini", 18, "#f59e0b"),
            new TrafficSource("Others", 7, "#64748b")
        );

        return new TrafficData(
            "2.4k",        // total visits
            "312",         // aiReferrals
            "+14.2%",      // growth
            sources        // source distribution
        );
    }
}