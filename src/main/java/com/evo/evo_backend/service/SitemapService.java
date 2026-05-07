package com.evo.evo_backend.service;

import com.evo.evo_backend.model.ServiceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class SitemapService {

    // Known AI/LLM bot user-agent identifiers
    private static final List<String> AI_BOT_AGENTS = List.of(
            "GPTBot", "ChatGPT-User", "CCBot", "anthropic-ai",
            "Claude-Web", "PerplexityBot", "YouBot", "Bytespider"
    );

    public ServiceResult analyze(String baseUrl) {

        List<String> issues = new ArrayList<>();
        List<String> highPriority = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority = new ArrayList<>();

        int score = 100;
        String origin = extractOrigin(baseUrl);

        // ================================
        // 🗺️ SITEMAP.XML
        // ================================
        boolean sitemapFound = false;
        boolean sitemapValid = false;
        int sitemapUrlCount = 0;

        try {
            Document sitemapDoc = Jsoup.connect(origin + "/sitemap.xml")
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .ignoreContentType(true)
                    .get();

            String sitemapContent = sitemapDoc.text();
            sitemapFound = true;

            if (sitemapContent.contains("<url>") || sitemapContent.contains("<sitemap>")) {
                sitemapValid = true;
                sitemapUrlCount = sitemapDoc.select("url").size();

                if (sitemapUrlCount == 0) {
                    score -= 10;
                    issues.add("Sitemap found but contains no <url> entries");
                    mediumPriority.add("Populate sitemap.xml with all important page URLs");
                }
            } else {
                score -= 15;
                issues.add("Sitemap.xml found but appears malformed");
                highPriority.add("Fix sitemap.xml format (must be valid XML with <urlset> and <url> tags)");
            }

        } catch (Exception e) {
            score -= 20;
            sitemapFound = false;
            issues.add("sitemap.xml not found or inaccessible");
            highPriority.add("Create and publish a sitemap.xml at " + origin + "/sitemap.xml");
        }

        // ================================
        // 🤖 ROBOTS.TXT
        // ================================
        boolean robotsFound = false;
        boolean aiBotsBlocked = false;
        List<String> blockedBots = new ArrayList<>();

        try {
            Document robotsDoc = Jsoup.connect(origin + "/robots.txt")
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .ignoreContentType(true)
                    .get();

            String robotsContent = robotsDoc.text();
            robotsFound = true;

            // Check if any AI bots are disallowed
            for (String bot : AI_BOT_AGENTS) {
                if (robotsContent.contains("User-agent: " + bot) ||
                    robotsContent.contains("user-agent: " + bot.toLowerCase())) {

                    // Check if followed by Disallow: /
                    int botIndex = robotsContent.toLowerCase().indexOf(bot.toLowerCase());
                    String afterBot = robotsContent.substring(botIndex, Math.min(botIndex + 200, robotsContent.length()));

                    if (afterBot.contains("Disallow: /") || afterBot.contains("disallow: /")) {
                        blockedBots.add(bot);
                        aiBotsBlocked = true;
                    }
                }
            }

            if (aiBotsBlocked) {
                score -= 30;
                issues.add("AI bots blocked in robots.txt: " + String.join(", ", blockedBots));
                highPriority.add("Allow AI crawlers in robots.txt (GPTBot, anthropic-ai, PerplexityBot, etc.)");
            }

            // Check sitemapUrl declared in robots.txt
            if (!robotsContent.toLowerCase().contains("sitemap:")) {
                score -= 5;
                issues.add("robots.txt does not declare sitemap URL");
                lowPriority.add("Add 'Sitemap: " + origin + "/sitemap.xml' to robots.txt");
            }

        } catch (Exception e) {
            score -= 10;
            issues.add("robots.txt not found or inaccessible");
            mediumPriority.add("Create a robots.txt file at " + origin + "/robots.txt");
        }

        score = Math.max(0, Math.min(score, 100));

        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        return new ServiceResult("sitemap", score, issues, fixes);
    }

    private String extractOrigin(String url) {
        try {
            URL parsed = new URL(url);
            return parsed.getProtocol() + "://" + parsed.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}