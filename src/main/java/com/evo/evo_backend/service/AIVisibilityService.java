package com.evo.evo_backend.service;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class AIVisibilityService {

    public int calculate(Document doc, int aiSeoScore, int contentScore) {

        int score = 100;

        String text = doc.text().toLowerCase();
        String html = doc.html().toLowerCase();

        // =========================
        // 1️⃣ ANSWERABILITY (CRITICAL)
        // =========================
        boolean hasQuestions = text.contains("?");
        boolean hasFAQ = text.contains("faq");
        boolean hasHowWhat = text.matches(".*(what is|how to|why|best).*");

        if (!hasQuestions) score -= 20;
        if (!hasFAQ) score -= 15;
        if (!hasHowWhat) score -= 15;

        // =========================
        // 2️⃣ STRUCTURE (CHUNKING)
        // =========================
        int headingCount = doc.select("h1, h2, h3").size();
        int listCount = doc.select("ul, ol").size();

        if (headingCount < 3) score -= 15;
        if (listCount < 2) score -= 10;

        // =========================
        // 3️⃣ ENTITY TRUST
        // =========================
        boolean hasSchema = doc.select("script[type=application/ld+json]").size() > 0;
        String siteName = doc.select("meta[property=og:site_name]").attr("content");

        if (!hasSchema) score -= 15;
        if (siteName == null || siteName.isEmpty()) score -= 10;

        // =========================
        // 4️⃣ JS BLOCK CHECK
        // =========================
        boolean isJSBlocked =
                text.length() < 120 &&
                (html.contains("id=\"root\"") ||
                 html.contains("id=\"app\"") ||
                 html.contains("__next_data__"));

        if (isJSBlocked) score -= 30;

        // =========================
        // 5️⃣ CONTENT DEPTH
        // =========================
        int contentLength = doc.text().length();

        if (contentLength < 500) score -= 20;
        else if (contentLength < 1000) score -= 10;

        // =========================
        // 🔥 COMBINE WITH AI SEO
        // =========================
        score = (int) Math.round(
                score * 0.6 +
                aiSeoScore * 0.4
        );

        // =========================
        // HARD CAPS
        // =========================
        if (contentScore < 40) score = Math.min(score, 45);
        if (aiSeoScore < 40) score = Math.min(score, 50);

        return Math.max(10, Math.min(score, 100));
    }
}