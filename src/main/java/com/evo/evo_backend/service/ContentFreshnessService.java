package com.evo.evo_backend.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class ContentFreshnessService {

    public ServiceResult analyze(Document doc, String url) {
        List<String> issues = new ArrayList<>();
        List<String> fixes = new ArrayList<>();
        int score = 100;

        // 1. Detect Meta Dates (SEO Standards)
        String published = doc.select("meta[property=article:published_time], meta[name=date], meta[name=publish-date]").attr("content");
        String modified = doc.select("meta[property=article:modified_time], meta[name=last-modified]").attr("content");
        boolean hasVisibleDate = !doc.select("time[datetime], .date, .published, .updated").isEmpty();

        // 2. Freshness Decay Logic
        if (published.isEmpty() && modified.isEmpty()) {
            score -= 30;
            issues.add("No publication or modification dates found in metadata");
            fixes.add("Implement article:published_time and article:modified_time Open Graph tags");
        } else {
            try {
                LocalDate targetDate = parseDate(modified.isEmpty() ? published : modified);
                long daysOld = ChronoUnit.DAYS.between(targetDate, LocalDate.now());

                if (daysOld > 730) {
                    score -= 40;
                    issues.add("Content is critically outdated (2+ years old)");
                    fixes.add("Perform a complete content refresh to stay relevant in AI search");
                } else if (daysOld > 365) {
                    score -= 20;
                    issues.add("Content is over 1 year old");
                    fixes.add("Review statistics and links to ensure accuracy");
                }
            } catch (Exception e) {
                score -= 10;
                issues.add("Date metadata is present but use an unreadable format");
                fixes.add("Use ISO 8601 (YYYY-MM-DD) for all date metadata");
            }
        }

        // 3. Search Intent Signal (Current Year)
        int currentYear = LocalDate.now().getYear();
        if (!doc.text().contains(String.valueOf(currentYear))) {
            score -= 10;
            issues.add("Content does not reference the current year (" + currentYear + ")");
            fixes.add("Update title tags or intro text to include the current year for better CTR");
        }

        if (!hasVisibleDate) {
            score -= 10;
            issues.add("Date is hidden from users (No visible <time> tag)");
            fixes.add("Display the 'Last Updated' date clearly to build E-E-A-T trust");
        }

        score = Math.max(0, Math.min(score, 100));
        return new ServiceResult("Content Freshness", score, issues, fixes);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) throw new RuntimeException("Empty date string");
        try {
            // Standardize ISO strings (remove timestamp if present)
            if (dateStr.contains("T")) dateStr = dateStr.split("T")[0];
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Unsupported format");
        }
    }
}