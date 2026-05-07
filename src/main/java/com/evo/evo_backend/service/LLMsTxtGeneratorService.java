package com.evo.evo_backend.service;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LLMsTxtGeneratorService {

    /**
     * Using Gemini 2.5 Flash for optimal performance/stability in 2026.
     */
    private static final String MODEL_ID = "gemini-2.5-flash";

    /**
     * Updated to 'gemini.api.keys' to match your properties file.
     * The ':' provides an empty default to prevent startup crashes.
     */
    @Value("${gemini.api.keys:}")
    private String apiKeys;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // =========================
    // 🚀 MAIN GENERATE METHOD
    // =========================
    public String generate(String inputUrl) {

        // Fix URL
        String url = inputUrl;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // =========================
        // 🌐 FETCH WEBSITE
        // =========================
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .header("Accept", "text/html")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
        } catch (Exception e) {
            return buildFallbackLLMsTxt(extractDomain(url), url);
        }

        // =========================
        // 🧠 EXTRACT SITE DATA
        // =========================
        SiteData data = extractSiteData(doc, url);

        // =========================
        // 🤖 GENERATE WITH GEMINI
        // =========================
        String geminiSummary = callGemini(data);

        // =========================
        // 📄 BUILD llms.txt
        // =========================
        return buildLLMsTxt(data, geminiSummary, url);
    }

    // =========================
    // 🔍 EXTRACT SITE DATA
    // =========================
    private SiteData extractSiteData(Document doc, String url) {

        SiteData data = new SiteData();

        // Domain
        data.domain = extractDomain(url);

        // Company name - OG first, then JSON-LD, then title
        data.companyName = doc.select("meta[property=og:site_name]").attr("content");

        if (data.companyName.isEmpty()) {
            for (Element script : doc.select("script[type=application/ld+json]")) {
                try {
                    JSONObject obj = new JSONObject(script.html().trim());
                    if (obj.has("name")) {
                        data.companyName = obj.getString("name");
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (data.companyName.isEmpty()) {
            String title = doc.title();
            if (title != null && !title.isEmpty()) {
                data.companyName = title.split("[|\\-–]")[0].trim();
            }
        }

        if (data.companyName.isEmpty()) data.companyName = data.domain;

        // Meta description
        data.metaDescription = doc.select("meta[name=description]").attr("content");
        if (data.metaDescription.isEmpty()) {
            data.metaDescription = doc.select("meta[property=og:description]").attr("content");
        }

        // Keywords
        data.keywords = doc.select("meta[name=keywords]").attr("content");

        // Page title
        data.pageTitle = doc.title();

        // H1
        Element h1 = doc.selectFirst("h1");
        data.h1 = h1 != null ? h1.text() : "";

        // H2s - up to 5
        List<String> h2s = new ArrayList<>();
        for (Element h2 : doc.select("h2")) {
            if (h2s.size() >= 5) break;
            String text = h2.text().trim();
            if (!text.isEmpty()) h2s.add(text);
        }
        data.h2s = h2s;

        // Navigation links - key pages
        List<String> navLinks = new ArrayList<>();
        for (Element link : doc.select("nav a, header a")) {
            String href = link.attr("href");
            String text = link.text().trim();
            if (!href.isEmpty() && !text.isEmpty()
                    && !href.startsWith("#")
                    && !href.startsWith("javascript")
                    && navLinks.size() < 8) {

                // Make absolute
                if (href.startsWith("/")) {
                    href = "https://" + data.domain + href;
                }
                navLinks.add(text + ": " + href);
            }
        }
        data.navLinks = navLinks;

        // Content snippet
        Document cleanDoc = doc.clone();
        cleanDoc.select("script, style, nav, footer, header").remove();
        String bodyText = cleanDoc.text().replaceAll("[^\\x00-\\x7F]", "");
        data.contentSnippet = bodyText.length() > 1500
                ? bodyText.substring(0, 1500)
                : bodyText;

        // Social links
        List<String> socials = new ArrayList<>();
        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href").toLowerCase();
            if (href.contains("twitter.com") || href.contains("x.com"))     socials.add("Twitter/X: " + link.attr("href"));
            if (href.contains("linkedin.com"))  socials.add("LinkedIn: " + link.attr("href"));
            if (href.contains("github.com"))    socials.add("GitHub: " + link.attr("href"));
            if (href.contains("facebook.com"))  socials.add("Facebook: " + link.attr("href"));
            if (href.contains("instagram.com")) socials.add("Instagram: " + link.attr("href"));
        }
        // Deduplicate socials
        data.socialLinks = socials.stream().distinct().limit(5).toList();

        // Schema type
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JSONObject obj = new JSONObject(script.html().trim());
                if (obj.has("@type")) {
                    data.schemaType = obj.getString("@type");
                    break;
                }
            } catch (Exception ignored) {}
        }

        return data;
    }

    // =========================
    // 🤖 CALL GEMINI
    // =========================
    private String callGemini(SiteData data) {
        
        // Use the first key from the comma-separated list
        String activeKey = (apiKeys != null && apiKeys.contains(",")) 
                ? apiKeys.split(",")[0].trim() 
                : apiKeys;

        String prompt = """
                You are an expert in AI visibility and LLM optimization.

                Based on the website data below, generate a concise description for an llms.txt file.

                Return EXACTLY this format (no extra text):

                TAGLINE: (one powerful line describing what this site does)
                ABOUT: (2-3 sentences describing the site, audience, and value)
                TOPICS: (comma separated list of 5-8 key topics this site covers)
                CITATION: (how AI should refer to this site when citing it)
                AUDIENCE: (who is this site for, 1 line)

                Website Data:
                Company: """ + data.companyName + """

                Title: """ + data.pageTitle + """

                Description: """ + data.metaDescription + """

                H1: """ + data.h1 + """

                Key Sections: """ + String.join(", ", data.h2s) + """

                Content Sample: """ + data.contentSnippet.substring(0,
                        Math.min(data.contentSnippet.length(), 800));

        try {
            JSONObject requestBody = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject().put("text", prompt)))));

            /**
             * Stable V1 Endpoint (Production Ready)
             */
            String apiUrl = "https://generativelanguage.googleapis.com/v1/models/"
                    + MODEL_ID + ":generateContent?key=" + activeKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                System.err.println("[EVO-LLM] Gemini Error " + response.statusCode() + ": " + response.body());
                return null;
            }

            JSONObject json     = new JSONObject(response.body());
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            if (content == null) return null;

            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.isEmpty()) return null;

            return parts.getJSONObject(0).optString("text", null);

        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // 📄 BUILD llms.txt
    // =========================
    private String buildLLMsTxt(SiteData data, String geminiRaw, String url) {

        // Parse Gemini response
        String tagline  = "";
        String about    = "";
        String topics   = "";
        String citation = "";
        String audience = "";

        if (geminiRaw != null) {
            for (String line : geminiRaw.split("\n")) {
                line = line.trim();
                if (line.startsWith("TAGLINE:"))  tagline  = line.replace("TAGLINE:", "").trim();
                if (line.startsWith("ABOUT:"))    about    = line.replace("ABOUT:", "").trim();
                if (line.startsWith("TOPICS:"))   topics   = line.replace("TOPICS:", "").trim();
                if (line.startsWith("CITATION:")) citation = line.replace("CITATION:", "").trim();
                if (line.startsWith("AUDIENCE:")) audience = line.replace("AUDIENCE:", "").trim();
            }
        }

        // Fallbacks if Gemini failed
        if (tagline.isEmpty())  tagline  = data.metaDescription.isEmpty()
                ? data.companyName + " - " + data.h1
                : data.metaDescription;
        if (about.isEmpty())    about    = data.metaDescription;
        if (topics.isEmpty())   topics   = String.join(", ", data.h2s);
        if (citation.isEmpty()) citation = data.companyName + " (" + data.domain + ")";
        if (audience.isEmpty()) audience = "General audience";

        // =========================
        // 📝 ASSEMBLE FILE
        // =========================
        StringBuilder sb = new StringBuilder();

        sb.append("# llms.txt\n");
        sb.append("# Generated by EVO - AI Visibility Platform (evo.ai)\n");
        sb.append("# This file helps AI systems understand your website\n");
        sb.append("\n");

        // Tagline block
        sb.append("> ").append(tagline).append("\n");
        sb.append("\n");

        // About section
        sb.append("## About\n");
        sb.append(about).append("\n");
        sb.append("\n");

        // Audience
        sb.append("## Audience\n");
        sb.append(audience).append("\n");
        sb.append("\n");

        // Key Topics
        if (!topics.isEmpty()) {
            sb.append("## Key Topics\n");
            for (String topic : topics.split(",")) {
                topic = topic.trim();
                if (!topic.isEmpty()) sb.append("- ").append(topic).append("\n");
            }
            sb.append("\n");
        }

        // Key Pages
        if (!data.navLinks.isEmpty()) {
            sb.append("## Key Pages\n");
            for (String link : data.navLinks) {
                sb.append("- ").append(link).append("\n");
            }
            sb.append("\n");
        }

        // Social / External Links
        if (!data.socialLinks.isEmpty()) {
            sb.append("## Social & External Links\n");
            for (String social : data.socialLinks) {
                sb.append("- ").append(social).append("\n");
            }
            sb.append("\n");
        }

        // Schema type if found
        if (!data.schemaType.isEmpty()) {
            sb.append("## Entity Type\n");
            sb.append("- Schema.org Type: ").append(data.schemaType).append("\n");
            sb.append("\n");
        }

        // Preferred Citation
        sb.append("## Preferred Citation\n");
        sb.append("\"").append(citation).append("\"\n");
        sb.append("\n");

        // AI Access instructions
        sb.append("## AI Access\n");
        sb.append("- Crawling: Allowed\n");
        sb.append("- Training: Allowed\n");
        sb.append("- Citation: Required\n");
        sb.append("- Source URL: ").append(url).append("\n");

        return sb.toString();
    }

    // =========================
    // 🛡️ FALLBACK llms.txt
    // =========================
    private String buildFallbackLLMsTxt(String domain, String url) {
        return "# llms.txt\n"
                + "# Generated by EVO - AI Visibility Platform\n"
                + "\n"
                + "> " + domain + " - Website content not accessible for analysis\n"
                + "\n"
                + "## About\n"
                + "This site could not be analyzed automatically. Please update manually.\n"
                + "\n"
                + "## AI Access\n"
                + "- Crawling: Allowed\n"
                + "- Training: Allowed\n"
                + "- Citation: Required\n"
                + "- Source URL: " + url + "\n";
    }

    // =========================
    // 🔧 DOMAIN EXTRACTOR
    // =========================
    private String extractDomain(String url) {
        try {
            return new URL(url).getHost().replace("www.", "");
        } catch (Exception e) {
            return "unknown.com";
        }
    }

    // =========================
    // 📦 SITE DATA HOLDER
    // =========================
    private static class SiteData {
        String       domain        = "";
        String       companyName   = "";
        String       metaDescription = "";
        String       keywords      = "";
        String       pageTitle     = "";
        String       h1            = "";
        List<String> h2s           = new ArrayList<>();
        List<String> navLinks      = new ArrayList<>();
        List<String> socialLinks   = new ArrayList<>();
        String       contentSnippet = "";
        String       schemaType    = "";
    }
}