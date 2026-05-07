package com.evo.evo_backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class LLMCrawlabilityService {

    // ================================
    // 🤖 KNOWN AI BOTS
    // ================================
    private static final Map<String, String> AI_BOTS = new LinkedHashMap<>();

    static {
        AI_BOTS.put("GPTBot",          "OpenAI (ChatGPT)");
        AI_BOTS.put("Claude-Web",      "Anthropic (Claude)");
        AI_BOTS.put("PerplexityBot",   "Perplexity AI");
        AI_BOTS.put("Googlebot",       "Google (Gemini)");
        AI_BOTS.put("CCBot",           "Common Crawl (LLM Training)");
        AI_BOTS.put("anthropic-ai",    "Anthropic Crawler");
        AI_BOTS.put("cohere-ai",       "Cohere AI");
        AI_BOTS.put("meta-externalagent", "Meta AI");
    }

    public ServiceResult analyze(String siteUrl) {

        List<String> issues        = new ArrayList<>();
        List<String> highPriority  = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority   = new ArrayList<>();

        int score = 100;

        // ================================
        // 📄 FETCH robots.txt
        // ================================
        String robotsTxt = fetchRobotsTxt(siteUrl);

        if (robotsTxt == null) {
            // Cannot fetch robots.txt at all
            score -= 20;
            issues.add("robots.txt is missing or unreachable");
            highPriority.add("Create a robots.txt file at your domain root");
            highPriority.add("Explicitly allow AI bots: GPTBot, Claude-Web, PerplexityBot, CCBot");

            return buildResult(score, issues, highPriority, mediumPriority, lowPriority);
        }

        // ================================
        // 🔍 CHECK EACH AI BOT
        // ================================
        List<String> blockedBots  = new ArrayList<>();
        List<String> allowedBots  = new ArrayList<>();
        List<String> missingBots  = new ArrayList<>();

        for (Map.Entry<String, String> bot : AI_BOTS.entrySet()) {
            String botName    = bot.getKey();
            String botLabel   = bot.getValue();
            BotStatus status  = checkBotStatus(robotsTxt, botName);

            switch (status) {
                case BLOCKED  -> blockedBots.add(botLabel + " (" + botName + ")");
                case ALLOWED  -> allowedBots.add(botLabel);
                case MISSING  -> missingBots.add(botLabel + " (" + botName + ")");
            }
        }

        // ================================
        // 🚨 SCORE BLOCKED BOTS
        // ================================
        if (!blockedBots.isEmpty()) {
            int deduction = Math.min(blockedBots.size() * 12, 50);
            score -= deduction;

            for (String bot : blockedBots) {
                issues.add("AI bot blocked in robots.txt: " + bot);
            }

            highPriority.add("Unblock AI bots in robots.txt: " +
                    String.join(", ", blockedBots));
        }

        // ================================
        // ⚠️ SCORE MISSING BOTS
        // ================================
        if (!missingBots.isEmpty()) {
            int deduction = Math.min(missingBots.size() * 4, 20);
            score -= deduction;

            issues.add("These AI bots have no explicit rule in robots.txt: " +
                    String.join(", ", missingBots));

            mediumPriority.add("Explicitly allow AI bots in robots.txt: " +
                    String.join(", ", missingBots));
        }

        // ================================
        // 🌐 CHECK WILDCARD BLOCK (* Disallow: /)
        // ================================
        boolean wildcardBlocked = isWildcardBlocked(robotsTxt);
        if (wildcardBlocked) {
            score -= 25;
            issues.add("Wildcard (*) blocks all bots including AI crawlers");
            highPriority.add("Add explicit Allow rules for AI bots above the wildcard Disallow");
        }

        // ================================
        // 📄 CHECK llms.txt EXISTS
        // ================================
        boolean hasLLMsTxt = checkLLMsTxt(siteUrl);
        if (!hasLLMsTxt) {
            score -= 10;
            issues.add("No llms.txt file found (emerging AI standard)");
            lowPriority.add("Create /llms.txt to describe your site to AI systems");
        }

        // ================================
        // ✅ POSITIVE SIGNALS
        // ================================
        if (!allowedBots.isEmpty() && blockedBots.isEmpty()) {
            lowPriority.add("Good: AI bots allowed - " + String.join(", ", allowedBots));
        }

        score = Math.max(0, Math.min(score, 100));

        return buildResult(score, issues, highPriority, mediumPriority, lowPriority);
    }

    // ================================
    // 🔧 FETCH robots.txt
    // ================================
    private String fetchRobotsTxt(String siteUrl) {
        try {
            // Build robots.txt URL
            String cleanUrl = siteUrl;
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://" + cleanUrl;
            }

            URL url = new URL(cleanUrl);
            String robotsUrl = url.getProtocol() + "://" + url.getHost() + "/robots.txt";

            HttpURLConnection conn = (HttpURLConnection) new URL(robotsUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line.toLowerCase()).append("\n");
            }
            reader.close();

            return sb.toString();

        } catch (Exception e) {
            return null;
        }
    }

    // ================================
    // 🔍 CHECK SINGLE BOT STATUS
    // ================================
    private BotStatus checkBotStatus(String robotsTxt, String botName) {
        String lowerBot      = botName.toLowerCase();
        String[] lines       = robotsTxt.split("\n");

        boolean inBotBlock      = false;
        boolean inWildcardBlock = false;
        boolean botExplicitAllow   = false;
        boolean botExplicitBlock   = false;

        for (String line : lines) {
            line = line.trim();

            // Detect User-agent block
            if (line.startsWith("user-agent:")) {
                String agent = line.replace("user-agent:", "").trim();
                inBotBlock      = agent.equals(lowerBot);
                inWildcardBlock = agent.equals("*");
            }

            // Check disallow / allow inside bot block
            if (inBotBlock) {
                if (line.startsWith("disallow: /") || line.equals("disallow:/")) {
                    botExplicitBlock = true;
                }
                if (line.startsWith("allow: /") || line.equals("allow:/")) {
                    botExplicitAllow = true;
                }
            }
        }

        if (botExplicitBlock && !botExplicitAllow) return BotStatus.BLOCKED;
        if (botExplicitAllow)                       return BotStatus.ALLOWED;
        return BotStatus.MISSING;
    }

    // ================================
    // 🌐 WILDCARD BLOCK CHECK
    // ================================
    private boolean isWildcardBlocked(String robotsTxt) {
        String[] lines          = robotsTxt.split("\n");
        boolean  inWildcard     = false;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("user-agent:")) {
                String agent = line.replace("user-agent:", "").trim();
                inWildcard = agent.equals("*");
            }

            if (inWildcard) {
                if (line.startsWith("disallow: /") && !line.equals("disallow: /specific")) {
                    return true;
                }
            }
        }
        return false;
    }

    // ================================
    // 📄 CHECK llms.txt
    // ================================
    private boolean checkLLMsTxt(String siteUrl) {
        try {
            String cleanUrl = siteUrl;
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "https://" + cleanUrl;
            }

            URL url = new URL(cleanUrl);
            String llmsUrl = url.getProtocol() + "://" + url.getHost() + "/llms.txt";

            HttpURLConnection conn = (HttpURLConnection) new URL(llmsUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            return conn.getResponseCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }

    // ================================
    // 🏗️ BUILD RESULT
    // ================================
    private ServiceResult buildResult(
            int score,
            List<String> issues,
            List<String> highPriority,
            List<String> mediumPriority,
            List<String> lowPriority
    ) {
        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        return new ServiceResult("llmCrawlability", score, issues, fixes);
    }

    // ================================
    // 📊 BOT STATUS ENUM
    // ================================
    private enum BotStatus {
        ALLOWED,
        BLOCKED,
        MISSING
    }
}