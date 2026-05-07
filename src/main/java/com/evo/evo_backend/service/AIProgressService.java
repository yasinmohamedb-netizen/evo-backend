package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AuditResponse;

@Service
public class AIProgressService {

    public Map<String, Object> explain(AuditResponse oldRes, AuditResponse newRes) {
        int oldScore = (oldRes != null) ? oldRes.getScore() : 0;
        int newScore = (newRes != null) ? newRes.getScore() : 0;
        int diff = newScore - oldScore;

        List<String> insights = new ArrayList<>();
        
        // Growth driver comparison
        if (oldRes != null) {
            if (newRes.getSchemaCount() > oldRes.getSchemaCount()) insights.add("Structured data (schema) density improved");
            if (newRes.getContentScore() > oldRes.getContentScore()) insights.add("Content semantic depth has increased");
            if (newRes.getPerformanceScore() > oldRes.getPerformanceScore()) insights.add("Technical performance optimizations detected");
        }
        
        if (insights.isEmpty()) {
            insights.add(diff == 0 ? "Visibility baseline maintained successfully" : "Minor calibration adjustments detected");
        }

        // Actionable suggestions based on specific metrics
        List<String> actions = new ArrayList<>();
        if (newRes.getSchemaCount() < 3) {
            actions.add("Implement Organization or FAQ schema to improve AI trust signals");
        }
        if (newRes.getContentScore() < 75) {
            actions.add("Enhance readability: Break down long paragraphs for better AI parsing");
        }
        if (newRes.getAiVisibilityScore() < 60) {
            actions.add("Optimize for AI: Use 'What is' and 'How to' headers for snippet eligibility");
        }

        // Formatting the Strategic Report
        StringBuilder sb = new StringBuilder();
        if (diff > 0) sb.append("🚀 PERFORMANCE BOOST\n");
        else if (diff < 0) sb.append("📉 TREND ALERT\n");
        else sb.append("📊 STABILITY REPORT\n");

        sb.append(diff > 0 ? "Your visibility increased by " + diff + " points." : 
                 (diff < 0 ? "Your score adjusted by " + Math.abs(diff) + " points." : "Your AI visibility baseline remains steady."));
        
        sb.append("\n\n🔍 STRATEGIC INSIGHTS\n");
        for (String i : insights) sb.append("• ").append(i).append("\n");

        sb.append("\n💡 SUGGESTIONS & IDEAS\n");
        for (String a : actions) sb.append("• ").append(a).append("\n");

        Map<String, Object> result = new HashMap<>();
        result.put("explanation", sb.toString());
        result.put("previousScore", oldScore);
        result.put("currentScore", newScore);
        result.put("improvement", diff);
        
        return result;
    }
}