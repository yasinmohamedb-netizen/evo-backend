package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AIAnalysis;

@Service
public class AISeoService {

    @Autowired private GPTService gptService;

    // =========================
    // MAIN ENTRY
    // =========================
    public AISeoResult analyze(Document doc, String url, AIAnalysis aiAnalysis) {

        int entityScore   = calculateEntityScore(doc);
        int answerScore   = calculateAnswerabilityScore(doc);
        int chunkScore    = calculateChunkingScore(doc);
        int crawlScore    = calculateCrawlabilityScore(doc);
        int semanticScore = calculateSemanticScore(doc, aiAnalysis);

        int aiSeoScore = (int) Math.round(
                entityScore   * 0.25 +
                answerScore   * 0.25 +
                chunkScore    * 0.20 +
                crawlScore    * 0.15 +
                semanticScore * 0.15
        );

        aiSeoScore = Math.max(10, Math.min(aiSeoScore, 100));

        List<String> issues = new ArrayList<>();
        List<String> fixes  = new ArrayList<>();

        // =========================
        // ISSUE COLLECTION
        // =========================
        if (entityScore < 60) {
            issues.add("Weak entity definition for AI");
            fixes.add("Add Organization/LocalBusiness schema");
        }

        if (answerScore < 60) {
            issues.add("Content not optimized for AI answers");
            fixes.add("Add FAQ and question-based content");
        }

        if (chunkScore < 60) {
            issues.add("Poor content structure for AI parsing");
            fixes.add("Use headings, lists, and sections");
        }

        if (crawlScore < 60) {
            issues.add("AI bots may not crawl this site properly");
            fixes.add("Allow GPTBot, Claude, PerplexityBot in robots.txt");
        }

        if (semanticScore < 60) {
            issues.add("Content lacks semantic depth for AI understanding");
            fixes.add("Add detailed explanatory content");
        }

        return new AISeoResult(aiSeoScore, entityScore, answerScore, chunkScore, crawlScore, semanticScore, issues, fixes);
    }

    // =========================
    // 1️⃣ ENTITY SCORE
    // =========================
    private int calculateEntityScore(Document doc) {

        int score = 100;

        boolean hasSchema = doc.select("script[type=application/ld+json]").size() > 0;
        String siteName = doc.select("meta[property=og:site_name]").attr("content");

        if (!hasSchema) score -= 40;
        if (siteName == null || siteName.isEmpty()) score -= 30;

        return clamp(score);
    }

    // =========================
    // 2️⃣ ANSWERABILITY
    // =========================
    private int calculateAnswerabilityScore(Document doc) {

        int score = 100;

        boolean hasFAQ = doc.text().toLowerCase().contains("faq");
        boolean hasQuestions = doc.text().contains("?");
        boolean hasHowWhat = doc.text().toLowerCase().matches(".*(what is|how to|why).*");

        if (!hasFAQ) score -= 40;
        if (!hasQuestions) score -= 30;
        if (!hasHowWhat) score -= 20;

        return clamp(score);
    }

    // =========================
    // 3️⃣ CONTENT CHUNKING
    // =========================
    private int calculateChunkingScore(Document doc) {

        int score = 100;

        int headingCount = doc.select("h1, h2, h3").size();
        int listCount = doc.select("ul, ol").size();

        if (headingCount < 3) score -= 30;
        if (listCount < 2) score -= 20;

        return clamp(score);
    }

    // =========================
    // 4️⃣ AI CRAWLABILITY
    // =========================
    private int calculateCrawlabilityScore(Document doc) {

        int score = 100;

        String html = doc.html().toLowerCase();
        String text = doc.text();

        boolean isJSBlocked =
                text.length() < 120 &&
                (html.contains("id=\"root\"") ||
                 html.contains("id=\"app\"") ||
                 html.contains("__next_data__"));

        if (isJSBlocked) score -= 50;

        return clamp(score);
    }

    // =========================
    // 5️⃣ SEMANTIC DEPTH (AI)
    // =========================
    private int calculateSemanticScore(Document doc, AIAnalysis aiAnalysis) {

        int score = 100;

        int contentLength = doc.text().length();

        if (contentLength < 500) score -= 40;
        else if (contentLength < 1000) score -= 20;

        // Optional: GPT-based refinement
        try {
            if (aiAnalysis != null && aiAnalysis.getSummary() != null) {
                if (aiAnalysis.getSummary().length() < 100) {
                    score -= 20;
                }
            }
        } catch (Exception ignored) {}

        return clamp(score);
    }

    // =========================
    // HELPER
    // =========================
    private int clamp(int score) {
        return Math.max(10, Math.min(score, 100));
    }

    // =========================
    // RESULT CLASS
    // =========================
    public static class AISeoResult {

        public final int aiSeoScore;

        public final int entityScore;
        public final int answerScore;
        public final int chunkScore;
        public final int crawlScore;
        public final int semanticScore;

        public final List<String> issues;
        public final List<String> fixes;
        // ✅ ADD THIS
        public int getScore() {
            return aiSeoScore;
        }

        public AISeoResult(int aiSeoScore,
                           int entityScore,
                           int answerScore,
                           int chunkScore,
                           int crawlScore,
                           int semanticScore,
                           List<String> issues,
                           List<String> fixes) {

            this.aiSeoScore = aiSeoScore;
            this.entityScore = entityScore;
            this.answerScore = answerScore;
            this.chunkScore = chunkScore;
            this.crawlScore = crawlScore;
            this.semanticScore = semanticScore;
            this.issues = issues;
            this.fixes = fixes;
        }
    }
}