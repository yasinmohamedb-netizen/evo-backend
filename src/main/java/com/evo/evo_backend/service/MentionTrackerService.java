package com.evo.evo_backend.service;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.MentionReport;
import com.evo.evo_backend.model.MentionReport.QueryResult;

@Service
public class MentionTrackerService {

    /**
     * Using Gemini 2.5 Flash for 2026 stability and speed.
     */
    private static final String MODEL_ID = "gemini-2.5-flash";

    /**
     * FIXED: Changed to 'keys' to match your properties file.
     * Added ':' default to prevent app crash if property is missing.
     */
    @Value("${gemini.api.keys:}")
    private String apiKeys;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // =========================
    // 🔍 QUERY TEMPLATES
    // These simulate what real users ask AI
    // =========================
    private static final List<String> QUERY_TEMPLATES = List.of(
            "What is {domain} and what do they offer?",
            "Is {brand} a good tool for AI visibility?",
            "What are the best tools for making websites visible to AI?",
            "Which websites help with AI SEO optimization?",
            "What are the top platforms for LLM optimization?",
            "How can I make my website visible to ChatGPT and Perplexity?",
            "What tools help websites get cited by AI?",
            "Who are the leaders in AI search optimization?"
    );

    // =========================
    // 🚀 MAIN TRACK METHOD
    // =========================
    public String trackMention(String brandName, String platformData) {
        // Simple tracker for the brand in given text data
        String activeKey = getActiveApiKey();
        if (activeKey == null || activeKey.isBlank()) return "{\"error\": \"API key missing\"}";

        String prompt = "Analyze this data for mentions of: " + brandName + "\nData: " + platformData;
        return callGeminiSimple(prompt, activeKey);
    }

    public MentionReport track(String inputUrl) {

        // Fix URL
        String url = inputUrl;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String domain    = extractDomain(url);
        String brandName = extractBrandName(domain);

        List<QueryResult> queryResults        = new ArrayList<>();
        List<String>      competitorsMentioned = new ArrayList<>();
        List<String>      recommendations      = new ArrayList<>();

        int mentionCount = 0;
        int totalQueries = QUERY_TEMPLATES.size();

        // =========================
        // 🔄 RUN EACH QUERY
        // =========================
        for (String template : QUERY_TEMPLATES) {

            // Fill in brand/domain placeholders
            String query = template
                    .replace("{domain}", domain)
                    .replace("{brand}", brandName);

            // Ask Gemini
            String aiResponse = askGemini(query);

            if (aiResponse == null || aiResponse.isBlank()) {
                queryResults.add(new QueryResult(query, false, "AI unavailable", new ArrayList<>()));
                continue;
            }

            // Check if brand is mentioned
            boolean brandMentioned = isBrandMentioned(aiResponse, brandName, domain);
            if (brandMentioned) mentionCount++;

            // Extract other brands mentioned
            List<String> mentionedBrands = extractMentionedBrands(aiResponse, brandName);

            // Add competitors
            for (String competitor : mentionedBrands) {
                if (!competitorsMentioned.contains(competitor)) {
                    competitorsMentioned.add(competitor);
                }
            }

            // Truncate AI response for storage
            String truncatedResponse = aiResponse.length() > 500
                    ? aiResponse.substring(0, 500) + "..."
                    : aiResponse;

            queryResults.add(new QueryResult(
                    query,
                    brandMentioned,
                    truncatedResponse,
                    mentionedBrands
            ));
        }

        // =========================
        // 🧮 CITATION SCORE
        // =========================
        int citationScore = (int) Math.round(
                ((double) mentionCount / totalQueries) * 100
        );

        // =========================
        // 🏷️ CITATION LEVEL
        // =========================
        String citationLevel = citationScore >= 70 ? "HIGH"
                             : citationScore >= 40 ? "MEDIUM"
                             : citationScore >= 10 ? "LOW"
                             : "NONE";

        boolean mentioned = mentionCount > 0;

        // =========================
        // 💡 RECOMMENDATIONS
        // =========================
        recommendations = buildRecommendations(
                citationScore,
                mentioned,
                competitorsMentioned,
                brandName
        );

        return new MentionReport(
                domain,
                brandName,
                mentioned,
                citationScore,
                citationLevel,
                queryResults,
                competitorsMentioned,
                recommendations
        );
    }

    // =========================
    // 🤖 ASK GEMINI
    // =========================
    private String askGemini(String query) {
        String activeKey = getActiveApiKey();
        if (activeKey == null || activeKey.isBlank()) return null;

        try {
            String prompt = """
                    You are a helpful AI assistant.
                    Answer the following question honestly and concisely.
                    Mention specific tools, brands or websites if you know them.
                    Keep answer under 150 words.

                    Question: """ + query;

            JSONObject requestBody = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("text", prompt)))));

            /**
             * Updated to Stable /v1/ Endpoint
             */
            String apiUrl = "https://generativelanguage.googleapis.com/v1/models/"
                    + MODEL_ID + ":generateContent?key=" + activeKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) return null;

            JSONObject json      = new JSONObject(response.body());
            return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            return null;
        }
    }

    private String callGeminiSimple(String prompt, String key) {
        try {
            JSONObject requestBody = new JSONObject()
                    .put("contents", new JSONArray()
                            .put(new JSONObject()
                                    .put("parts", new JSONArray()
                                            .put(new JSONObject().put("text", prompt)))));

            String apiUrl = "https://generativelanguage.googleapis.com/v1/models/"
                    + MODEL_ID + ":generateContent?key=" + key;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            return new JSONObject(response.body())
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // 🔍 CHECK BRAND MENTIONED
    // =========================
    private boolean isBrandMentioned(String aiResponse, String brandName, String domain) {
        String lower = aiResponse.toLowerCase();
        return lower.contains(brandName.toLowerCase()) ||
               lower.contains(domain.toLowerCase());
    }

    // =========================
    // 🏢 EXTRACT COMPETITOR BRANDS
    // Uses regex to find capitalized brand-like words
    // =========================
    private List<String> extractMentionedBrands(String aiResponse, String ownBrand) {

        List<String> brands = new ArrayList<>();

        // Known AI/SEO tools to look for
        List<String> knownTools = List.of(
                "Semrush", "Ahrefs", "Moz", "Surfer", "Clearscope",
                "MarketMuse", "Frase", "BrightEdge", "Conductor",
                "Perplexity", "ChatGPT", "Claude", "Gemini",
                "Screaming Frog", "Sitebulb", "DeepCrawl",
                "Botify", "OnCrawl", "ContentKing",
                "RankMath", "Yoast", "Schema App"
        );

        String lowerResponse = aiResponse.toLowerCase();

        for (String tool : knownTools) {
            if (lowerResponse.contains(tool.toLowerCase()) &&
                !tool.equalsIgnoreCase(ownBrand)) {
                if (!brands.contains(tool)) {
                    brands.add(tool);
                }
            }
        }

        // Also extract capitalized words as potential brands
        Pattern pattern = Pattern.compile("\\b([A-Z][a-z]+(?:[A-Z][a-z]+)+)\\b");
        Matcher matcher = pattern.matcher(aiResponse);

        while (matcher.find()) {
            String word = matcher.group(1);
            if (!word.equalsIgnoreCase(ownBrand) &&
                !brands.contains(word) &&
                word.length() > 3) {
                brands.add(word);
            }
        }

        return brands.stream().limit(5).toList();
    }

    // =========================
    // 💡 BUILD RECOMMENDATIONS
    // =========================
    private List<String> buildRecommendations(
            int citationScore,
            boolean mentioned,
            List<String> competitors,
            String brandName
    ) {
        List<String> recs = new ArrayList<>();

        if (citationScore == 0) {
            recs.add("AI has no awareness of " + brandName + " - focus on brand building");
            recs.add("Publish authoritative content that AI systems can discover");
            recs.add("Get mentioned on high-authority sites (Forbes, TechCrunch, etc.)");
            recs.add("Add structured data to help AI understand your brand entity");
            recs.add("Build Wikipedia or Wikidata presence for entity recognition");
        } else if (citationScore < 40) {
            recs.add("AI awareness of " + brandName + " is low - increase content volume");
            recs.add("Target long-tail questions your audience asks AI tools");
            recs.add("Create comparison pages vs competitors AI is citing");
            recs.add("Add FAQ sections that directly answer common queries");
        } else if (citationScore < 70) {
            recs.add("Good AI awareness - focus on improving citation quality");
            recs.add("Create definitive guides AI can use as reference sources");
            recs.add("Get backlinks from sites AI commonly cites");
            recs.add("Optimize content freshness - update pages regularly");
        } else {
            recs.add("Excellent AI visibility - maintain content quality");
            recs.add("Monitor competitor movements in AI citations");
            recs.add("Expand to new topic clusters to grow citation surface");
        }

        if (!competitors.isEmpty()) {
            recs.add("Competitors being cited instead: " +
                    String.join(", ", competitors.subList(0, Math.min(3, competitors.size()))));
            recs.add("Analyze competitor content structure and match their depth");
        }

        return recs;
    }

    // =========================
    // 🔧 HELPERS
    // =========================
    private String getActiveApiKey() {
        return (apiKeys != null && apiKeys.contains(",")) 
                ? apiKeys.split(",")[0].trim() 
                : apiKeys;
    }

    private String extractBrandName(String domain) {
        String name = domain.contains(".")
                ? domain.substring(0, domain.lastIndexOf("."))
                : domain;

        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }

    private String extractDomain(String url) {
        try {
            return new URL(url).getHost().replace("www.", "");
        } catch (Exception e) {
            return url;
        }
    }
}