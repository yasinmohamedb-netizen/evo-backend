package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class SocialMetaService {

    public ServiceResult analyze(Document doc) {

        List<String> issues = new ArrayList<>();
        List<String> highPriority = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority = new ArrayList<>();

        int score = 100;

        // ================================
        // 📘 OPEN GRAPH TAGS
        // ================================
        String ogTitle = doc.select("meta[property=og:title]").attr("content");
        String ogDescription = doc.select("meta[property=og:description]").attr("content");
        String ogImage = doc.select("meta[property=og:image]").attr("content");
        String ogUrl = doc.select("meta[property=og:url]").attr("content");
        String ogType = doc.select("meta[property=og:type]").attr("content");
        String ogSiteName = doc.select("meta[property=og:site_name]").attr("content");

        boolean hasOgTitle = !ogTitle.isEmpty();
        boolean hasOgDesc = !ogDescription.isEmpty();
        boolean hasOgImage = !ogImage.isEmpty();
        boolean hasOgUrl = !ogUrl.isEmpty();

        if (!hasOgTitle) {
            score -= 15;
            issues.add("Missing og:title Open Graph tag");
            highPriority.add("Add <meta property=\"og:title\" content=\"...\"> to <head>");
        }

        if (!hasOgDesc) {
            score -= 15;
            issues.add("Missing og:description Open Graph tag");
            highPriority.add("Add <meta property=\"og:description\" content=\"...\"> to <head>");
        }

        if (!hasOgImage) {
            score -= 10;
            issues.add("Missing og:image Open Graph tag");
            mediumPriority.add("Add og:image with a 1200x630px image for rich previews");
        }

        if (!hasOgUrl) {
            score -= 5;
            issues.add("Missing og:url Open Graph tag");
            lowPriority.add("Add og:url to canonicalize the page URL");
        }

        if (ogType.isEmpty()) {
            score -= 3;
            issues.add("Missing og:type tag (e.g., website, article)");
            lowPriority.add("Add og:type (e.g., <meta property=\"og:type\" content=\"website\">)");
        }

        if (ogSiteName.isEmpty()) {
            score -= 2;
            lowPriority.add("Add og:site_name for brand consistency in social sharing");
        }

        // ================================
        // 🐦 TWITTER CARD TAGS
        // ================================
        String twitterCard = doc.select("meta[name=twitter:card]").attr("content");
        String twitterTitle = doc.select("meta[name=twitter:title]").attr("content");
        String twitterDesc = doc.select("meta[name=twitter:description]").attr("content");
        String twitterImage = doc.select("meta[name=twitter:image]").attr("content");

        boolean hasTwitterCard = !twitterCard.isEmpty();

        if (!hasTwitterCard) {
            score -= 10;
            issues.add("Missing twitter:card meta tag");
            mediumPriority.add("Add <meta name=\"twitter:card\" content=\"summary_large_image\">");
        } else {
            if (twitterTitle.isEmpty()) {
                score -= 5;
                issues.add("Missing twitter:title tag");
                mediumPriority.add("Add twitter:title meta tag");
            }
            if (twitterDesc.isEmpty()) {
                score -= 5;
                issues.add("Missing twitter:description tag");
                mediumPriority.add("Add twitter:description meta tag");
            }
            if (twitterImage.isEmpty()) {
                score -= 5;
                issues.add("Missing twitter:image tag");
                lowPriority.add("Add twitter:image for visual card previews on X/Twitter");
            }
        }

        // ================================
        // 🔗 CANONICAL URL
        // ================================
        String canonical = doc.select("link[rel=canonical]").attr("href");
        if (canonical.isEmpty()) {
            score -= 8;
            issues.add("Missing canonical URL tag");
            mediumPriority.add("Add <link rel=\"canonical\" href=\"...\"> to avoid duplicate content issues");
        }

        // ================================
        // 📰 ARTICLE META (for blog/news)
        // ================================
        String articleAuthor = doc.select("meta[property=article:author]").attr("content");
        String articlePublished = doc.select("meta[property=article:published_time]").attr("content");

        boolean looksLikeArticle = !doc.select("article").isEmpty()
                || ogType.equalsIgnoreCase("article");

        if (looksLikeArticle) {
            if (articleAuthor.isEmpty()) {
                score -= 5;
                issues.add("Article page missing article:author meta tag");
                lowPriority.add("Add article:author meta tag for article pages");
            }
            if (articlePublished.isEmpty()) {
                score -= 5;
                issues.add("Article page missing article:published_time meta tag");
                lowPriority.add("Add article:published_time for news/article content");
            }
        }

        score = Math.max(0, Math.min(score, 100));

        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        return new ServiceResult("socialMeta", score, issues, fixes);
    }
}