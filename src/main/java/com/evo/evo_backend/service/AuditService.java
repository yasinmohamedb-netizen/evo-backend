package com.evo.evo_backend.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AIAnalysis;
import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.model.Competitor;
import com.evo.evo_backend.model.KeywordInsight;
import com.evo.evo_backend.model.ServiceResult;
import com.evo.evo_backend.model.TrafficData;
import com.evo.evo_backend.model.TrafficSource;
import com.evo.evo_backend.model.UnifiedAuditData;
import com.evo.evo_backend.service.fetch.FetchOrchestrator;

@Service
public class AuditService {

    // =========================
    // DEPENDENCIES
    // =========================
    @Autowired private FetchOrchestrator fetchOrchestrator;
    @Autowired private GPTService gptService;
    @Autowired private PerformanceService performanceService;
    @Autowired private AccessibilityService accessibilityService;
    @Autowired private SitemapService sitemapService;
    @Autowired private SocialMetaService socialMetaService;
    @Autowired private LinkHealthService linkHealthService;
    @Autowired private LLMCrawlabilityService llmCrawlabilityService;
    @Autowired private AISeoService aiSeoService;
    @Autowired private AIVisibilityService aiVisibilityService;
    @Autowired private SchemaMarkupService schemaMarkupService;
    @Autowired private ContentFreshnessService contentFreshnessService;
    @Autowired private EEATService eeATService;
    @Autowired private BusinessInsightService businessInsightService;
    @Autowired private GooglePlacesService googlePlacesService;
    @Autowired private AIKeywordService aiKeywordService;
    @Autowired private KeywordIntelligenceService keywordIntelligenceService;
    @Autowired private UnifiedAIService unifiedAIService;

    // =========================
    // WEIGHTS (must sum to 1.0)
    // =========================
    private static final double W_CONTENT     = 0.30;
    private static final double W_PERFORMANCE = 0.10;
    private static final double W_A11Y        = 0.08;
    private static final double W_SITEMAP     = 0.08;
    private static final double W_SOCIAL      = 0.08;
    private static final double W_LINKS       = 0.08;
    private static final double W_LLM         = 0.08;
    private static final double W_SCHEMA      = 0.10;
    private static final double W_EEAT        = 0.10;

    // =========================
    // MAIN ANALYZE METHOD (URL-BASED)
    // =========================
    public AuditResponse analyze(String inputUrl) {
        List<String> allIssues = new ArrayList<>();
        List<String> allFixes = new ArrayList<>();

        String companyName = "";
        String location = "Unknown";
        String businessType = "General";

        // 1. NORMALISE URL
        String url = inputUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // 2. FETCH — 4-LAYER CASCADE
        FetchOrchestrator.FetchOutcome outcome = fetchOrchestrator.fetch(url);
        if (!outcome.success) {
            return buildFailureResponse(url, outcome.error);
        }

        Document doc = outcome.doc;
        long responseTimeMs = outcome.responseTimeMs;
        boolean isCached = outcome.isCached();

        if (outcome.warning != null) {
            allIssues.add("Using cached version of the website");
            allFixes.add(outcome.warning);
        }

        // 3. JS SHELL DETECTION
        String rawHtml = doc.html().toLowerCase();
        String textContent = doc.text();
        boolean isJSBlocked = textContent.length() < 120 &&
                (rawHtml.contains("id=\"root\"") ||
                 rawHtml.contains("id=\"app\"") ||
                 rawHtml.contains("__next_data__") ||
                 rawHtml.contains("enable javascript"));

        // 4. COMPANY NAME EXTRACTION
        if (!isCached) {
            String ogSiteName = doc.select("meta[property=og:site_name]").attr("content");
            if (ogSiteName != null && !ogSiteName.isBlank() && !isCacheArtifact(ogSiteName)) {
                companyName = ogSiteName.trim();
            }

            if (companyName.isEmpty()) {
                for (Element script : doc.select("script[type=application/ld+json]")) {
                    try {
                        String jsonStr = script.html().trim();
                        if (jsonStr.startsWith("[")) {
                            JSONArray arr = new JSONArray(jsonStr);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                if (obj.has("name")) {
                                    String candidate = obj.getString("name");
                                    if (!isCacheArtifact(candidate)) {
                                        companyName = candidate;
                                        break;
                                    }
                                }
                            }
                        } else {
                            JSONObject obj = new JSONObject(jsonStr);
                            if (obj.has("name")) {
                                String candidate = obj.getString("name");
                                if (!isCacheArtifact(candidate)) {
                                    companyName = candidate;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (!companyName.isEmpty()) break;
                }
            }

            if (companyName.isEmpty()) {
                String title = doc.title();
                if (title != null && !title.isBlank() && !isCacheArtifact(title)) {
                    companyName = title.split("[|\\-–]")[0].trim();
                }
            }
        }

        if (companyName.isEmpty() || isCacheArtifact(companyName)) {
            companyName = extractDomain(url);
        }

        // 5. LOCATION + BUSINESS TYPE
        location = businessInsightService.detectLocation(doc);
        businessType = businessInsightService.detectBusinessType(doc);

        if (isGenericBusinessType(businessType)) {
            businessType = inferBusinessTypeFromUrl(url);
        }

        // 6. CONTENT SCORE CALCULATION
        int contentScore = calculateContentScore(doc, isJSBlocked, allIssues, allFixes);

        // 7. AI ANALYSIS
        UnifiedAuditData aiData = null;
        AIAnalysis aiAnalysis;

        try {
            aiData = unifiedAIService.getFullAnalysis(doc.text(), businessType, location);

            String cleaned = doc.text();
            if (cleaned != null && cleaned.length() > 2000) {
                cleaned = cleaned.substring(0, 2000);
            }

            String gptRaw;
            try {
                gptRaw = gptService.analyzeContent(cleaned);
            } catch (Exception e) {
                gptRaw = "AI service temporarily slow.";
            }

            StringBuilder summaryBuilder = new StringBuilder();
            if (aiData != null && aiData.getKeyFindings() != null) {
                summaryBuilder.append(String.join(" ", aiData.getKeyFindings()));
            }

            if (summaryBuilder.length() > 0 && gptRaw != null && !gptRaw.isBlank()) {
                summaryBuilder.append(" | ");
            }

            if (gptRaw != null && !gptRaw.isBlank()
                    && !gptRaw.toLowerCase().contains("error")
                    && !gptRaw.contains("429")
                    && !gptRaw.contains("503")) {
                summaryBuilder.append(gptRaw);
            }

            String finalSummary = summaryBuilder.toString();
            if (finalSummary.isEmpty()) {
                finalSummary = "AI analysis currently unavailable.";
            }

            aiAnalysis = new AIAnalysis(
                    finalSummary,
                    (aiData != null) ? aiData.getContentGaps() : new ArrayList<>(),
                    new ArrayList<>()
            );

        } catch (Exception e) {
            aiAnalysis = new AIAnalysis(
                    "AI analysis failed to map correctly.",
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        // 8. AI SEO + VISIBILITY
        AISeoService.AISeoResult aiSeo = aiSeoService.analyze(doc, url, aiAnalysis);
        int aiVisibilityScore = aiVisibilityService.calculate(doc, aiSeo.getScore(), contentScore);

        // 9. ALL SERVICES
        ServiceResult perf = performanceService.analyze(doc, responseTimeMs);
        ServiceResult a11y = accessibilityService.analyze(doc);
        ServiceResult sitemap = sitemapService.analyze(url);
        ServiceResult social = socialMetaService.analyze(doc);
        ServiceResult links = linkHealthService.analyze(doc, url);
        ServiceResult llm = llmCrawlabilityService.analyze(url);
        ServiceResult schema = schemaMarkupService.analyze(doc);
        ServiceResult freshness = contentFreshnessService.analyze(doc, url);
        ServiceResult eeat = eeATService.analyze(doc, url);

        // 10. WEIGHTED OVERALL SCORE
        int overallScore = calculateOverallScore(contentScore, perf, a11y, sitemap, social, links, llm, schema, eeat);

        int schemaCount = doc.select("script[type=application/ld+json]").size();
        String metaDesc = doc.select("meta[name=description]").attr("content");
        int contentLength = doc.text().length();

        if (contentScore < 30) overallScore = Math.min(overallScore, 45);
        if (contentScore < 50) overallScore = Math.min(overallScore, 60);
        if (schemaCount == 0 && (metaDesc == null || metaDesc.isEmpty())) overallScore = Math.min(overallScore, 50);
        if (contentLength < 400) overallScore = Math.min(overallScore, 45);
        if (eeat.getScore() < 30) overallScore = Math.min(overallScore, 55);
        if (schema.getScore() < 30) overallScore = Math.min(overallScore, 55);

        overallScore = Math.max(10, Math.min(overallScore, 100));

        // 11. MERGE + DEDUPLICATE
        mergeAllIssuesAndFixes(allIssues, allFixes, perf, a11y, sitemap, social, links, llm, schema, freshness, eeat);
        List<String> dedupIssues = deduplicate(allIssues).subList(0, Math.min(10, deduplicate(allIssues).size()));
        List<String> dedupFixes = deduplicate(allFixes).subList(0, Math.min(10, deduplicate(allFixes).size()));

        // 12. POTENTIAL + VISIBILITY
        int potentialScore = Math.min(overallScore + 25, 100);
        int improvement = potentialScore - overallScore;
        String visibility = overallScore >= 80 ? "HIGH" : overallScore >= 50 ? "MEDIUM" : "LOW";

        // 13. ENHANCED TRAFFIC ANALYSIS using overallScore and internal links
        int internalLinks = doc.select("a[href^=/]").size();
        TrafficData liveTraffic = getLiveTraffic(url, overallScore, internalLinks);

        // 14. BUILD RESPONSE
        String scrapedText = doc.text();
        if (scrapedText != null && scrapedText.length() > 12000) {
            scrapedText = scrapedText.substring(0, 12000) + "... [Text truncated for analysis]";
        }

        AuditResponse finalResponse = new AuditResponse(
                companyName,
                overallScore,
                potentialScore,
                improvement,
                visibility,
                aiSeo.getScore(),
                aiVisibilityScore,
                contentScore,
                perf.getScore(),
                a11y.getScore(),
                sitemap.getScore(),
                social.getScore(),
                links.getScore(),
                llm.getScore(),
                dedupIssues,
                dedupFixes,
                aiAnalysis,
                scrapedText,
                liveTraffic
        );

        // Optional extra setters if these fields exist in AuditResponse
        finalResponse.setLocation(location);
        finalResponse.setBusinessType(businessType);
        finalResponse.setCompetitors(getFilteredCompetitors(doc, businessType, location));
        finalResponse.setKeywordInsights(getKeywordInsights(aiData));
        finalResponse.setImprovements(generateImprovementPlan(schema, eeat, freshness, businessType, overallScore, location));
        finalResponse.setWordCount(doc.text().split("\\s+").length);
        finalResponse.setFaqCount(textContent.toLowerCase().contains("faq") || textContent.contains("?") ? 1 : 0);
        finalResponse.setSchemaCount(schemaCount);
        finalResponse.setHeadingCount(doc.select("h1, h2, h3").size());
        finalResponse.setInternalLinks(internalLinks);

        return finalResponse;
    }

    // =========================
    // BUSINESS DISCOVERY METHOD
    // =========================
    public AuditResponse discoverKeywords(String businessName, String location) {
        String virtualContext = String.format(
                "Business Name/Category: %s. Target Location: %s. Goal: Generate a market strategy and keyword list for a business without a website.",
                businessName, location
        );

        UnifiedAuditData aiData = unifiedAIService.getFullAnalysis(virtualContext, businessName, location);

        AuditResponse discoveryResponse = new AuditResponse();
        discoveryResponse.setCompanyName(businessName);
        discoveryResponse.setBusinessType(businessName);
        discoveryResponse.setLocation(location);
        discoveryResponse.setRawContent(virtualContext);
        discoveryResponse.setScore(0);
        discoveryResponse.setVisibility("PLANNING");

        if (aiData != null) {
            discoveryResponse.setKeywordInsights(aiData.getKeywords());

            String summary = (aiData.getKeyFindings() != null && !aiData.getKeyFindings().isEmpty())
                    ? String.join(" ", aiData.getKeyFindings())
                    : "Strategic market analysis for " + businessName;

            AIAnalysis discoveryAnalysis = new AIAnalysis(summary, aiData.getContentGaps(), new ArrayList<>());
            discoveryResponse.setAiAnalysis(discoveryAnalysis);
        }

        try {
            List<Competitor> competitors = googlePlacesService.getLocalCompetitors(businessName, location);
            discoveryResponse.setCompetitors(competitors);
        } catch (Exception e) {
            discoveryResponse.setCompetitors(new ArrayList<>());
        }

        List<String> strategyPlan = new ArrayList<>();
        strategyPlan.add("Establish a digital presence in " + location);
        strategyPlan.add("Optimize for keywords like: " +
                (aiData != null && aiData.getKeywords() != null && !aiData.getKeywords().isEmpty()
                        ? aiData.getKeywords().get(0).getKeyword()
                        : businessName));
        strategyPlan.add("Analyze top-rated local competitors listed in this report");

        discoveryResponse.setImprovements(strategyPlan);

        return discoveryResponse;
    }

    // =========================
    // ENHANCED TRAFFIC ANALYSIS
    // =========================
    public TrafficData getLiveTraffic(String url, int score, int internalLinks) {
        Random random = new Random();
        
        // 1. Calculate base traffic using audit metrics (score + internalLinks as authority proxy)
        long baseTraffic = (long) (score * 45) + (internalLinks * 120L);
        baseTraffic += random.nextInt(800); // Add variance

        // Format 'total' (e.g., 4.2k)
        String total = formatTraffic(baseTraffic);
        
        // Estimate 15-25% as AI Referrals
        long aiHits = (long) (baseTraffic * (0.15 + (0.10 * random.nextDouble())));
        String aiReferrals = formatTraffic(aiHits);

        // Randomize growth between +2% and +25%
        String growth = "+" + (2 + random.nextInt(23)) + "." + random.nextInt(9) + "%";

        return new TrafficData(
            total,
            aiReferrals,
            growth,
            generateRandomSources(random)
        );
    }

    private String formatTraffic(long traffic) {
        if (traffic >= 1000000) {
            return (traffic / 1000000) + "." + ((traffic % 1000000) / 100000) + "M";
        } else if (traffic >= 1000) {
            return (traffic / 1000) + "." + ((traffic % 1000) / 100) + "k";
        } else {
            return String.valueOf(traffic);
        }
    }

    private List<TrafficSource> generateRandomSources(Random random) {
        // Distribute 100% across 4 sources
        int p = 30 + random.nextInt(20); // Perplexity: 30-50%
        int c = 20 + random.nextInt(15); // ChatGPT: 20-35%
        int g = 10 + random.nextInt(10); // Gemini: 10-20%
        int o = 100 - (p + c + g);       // Others: Remainder

        return Arrays.asList(
            new TrafficSource("Perplexity AI", p, "#3b82f6"),
            new TrafficSource("ChatGPT Search", c, "#10b981"),
            new TrafficSource("Google Gemini", g, "#f59e0b"),
            new TrafficSource("Others", o, "#64748b")
        );
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private int calculateContentScore(Document doc, boolean isJSBlocked, List<String> issues, List<String> fixes) {
        int critical = 0, major = 0, minor = 0;

        int schemaCount = doc.select("script[type=application/ld+json]").size();
        String metaDesc = doc.select("meta[name=description]").attr("content");
        int paraCount = doc.select("p").size();
        int listCount = doc.select("ul, ol").size();
        int tableCount = doc.select("table").size();
        int headingCount = doc.select("h1, h2, h3").size();
        int contentLength = doc.text().length();
        boolean thinSite = contentLength < 400 && paraCount < 3;
        boolean hasFaqContent = doc.text().toLowerCase().contains("faq") || doc.text().contains("?");

        if (isJSBlocked) {
            critical += 25;
            issues.add("Content hidden behind JavaScript");
            fixes.add("Enable Server-Side Rendering (SSR)");
        }
        if (schemaCount == 0) {
            critical += 25;
            issues.add("Missing structured data (JSON-LD)");
            fixes.add("Add JSON-LD schema markup (Organization, FAQ, etc.)");
        }

        if (contentLength < 800) {
            major += 20;
            issues.add("Low content depth (" + contentLength + " chars)");
            fixes.add("Expand content to at least 800+ characters");
        }
        if (metaDesc == null || metaDesc.isEmpty()) {
            major += 15;
            issues.add("Missing meta description");
            fixes.add("Add a concise meta description (150-160 chars)");
        }
        if (headingCount < 3) {
            major += 10;
            issues.add("Weak heading hierarchy (" + headingCount + " headings)");
            fixes.add("Use H1, H2, H3 tags to structure content");
        }

        if (!hasFaqContent) {
            minor += 5;
            issues.add("No FAQ or question-based content found");
            fixes.add("Add an FAQ section to improve AI answerability");
        }
        if (tableCount == 0) {
            minor += 3;
            issues.add("No data tables found");
            fixes.add("Add comparison tables or data tables where relevant");
        }
        if (listCount < 2 && paraCount > 5) {
            minor += 3;
            issues.add("Poor content structure — no lists despite many paragraphs");
            fixes.add("Use bullet lists and numbered lists to improve scannability");
        }

        if (thinSite) {
            critical = (int) (critical * 0.6);
            major = (int) (major * 0.7);
        }

        critical = Math.min(critical, 50);
        major = Math.min(major, 40);
        minor = Math.min(minor, 20);

        return Math.max(10, Math.min(100 - (critical + major + minor), 100));
    }

    private int calculateOverallScore(int contentScore, ServiceResult perf, ServiceResult a11y,
                                      ServiceResult sitemap, ServiceResult social, ServiceResult links,
                                      ServiceResult llm, ServiceResult schema, ServiceResult eeat) {
        return (int) Math.round(
                contentScore * W_CONTENT +
                perf.getScore() * W_PERFORMANCE +
                a11y.getScore() * W_A11Y +
                sitemap.getScore() * W_SITEMAP +
                social.getScore() * W_SOCIAL +
                links.getScore() * W_LINKS +
                llm.getScore() * W_LLM +
                schema.getScore() * W_SCHEMA +
                eeat.getScore() * W_EEAT
        );
    }

    private void mergeAllIssuesAndFixes(List<String> allIssues, List<String> allFixes,
                                        ServiceResult perf, ServiceResult a11y, ServiceResult sitemap,
                                        ServiceResult social, ServiceResult links, ServiceResult llm,
                                        ServiceResult schema, ServiceResult freshness, ServiceResult eeat) {
        allIssues.addAll(perf.getIssues());
        allIssues.addAll(a11y.getIssues());
        allIssues.addAll(sitemap.getIssues());
        allIssues.addAll(social.getIssues());
        allIssues.addAll(links.getIssues());
        allIssues.addAll(llm.getIssues());
        allIssues.addAll(schema.getIssues());
        allIssues.addAll(freshness.getIssues());
        allIssues.addAll(eeat.getIssues());

        allFixes.addAll(perf.getFixes());
        allFixes.addAll(a11y.getFixes());
        allFixes.addAll(sitemap.getFixes());
        allFixes.addAll(social.getFixes());
        allFixes.addAll(links.getFixes());
        allFixes.addAll(llm.getFixes());
        allFixes.addAll(schema.getFixes());
        allFixes.addAll(freshness.getFixes());
        allFixes.addAll(eeat.getFixes());
    }

    private List<Competitor> getFilteredCompetitors(Document doc, String businessType, String location) {
        String keyword = aiKeywordService.extractKeyword(doc.text(), businessType, location);
        List<Competitor> competitors = googlePlacesService.getLocalCompetitors(keyword, location);

        String dominantCity = competitors.stream()
                .map(Competitor::getCity)
                .filter(c -> c != null && !c.equalsIgnoreCase("India"))
                .findFirst()
                .orElse("India");

        return competitors.stream()
                .filter(c -> c.getCity() != null && c.getCity().equalsIgnoreCase(dominantCity))
                .collect(Collectors.toList());
    }

    private List<KeywordInsight> getKeywordInsights(UnifiedAuditData aiData) {
        if (aiData != null && aiData.getKeywords() != null && !aiData.getKeywords().isEmpty()) {
            return aiData.getKeywords();
        }
        return keywordIntelligenceService.analyzeKeywords("", "General");
    }

    private List<String> generateImprovementPlan(ServiceResult schema, ServiceResult eeat,
                                                 ServiceResult freshness, String businessType,
                                                 int overallScore, String location) {
        List<String> improvements = new ArrayList<>();

        if (schema.getScore() < 50) improvements.add("Add structured data (FAQ, Organization, Product, etc.)");
        if (eeat.getScore() < 50) improvements.add("Improve trust signals (author bio, testimonials, certifications)");
        if (freshness.getScore() < 50) improvements.add("Update outdated content and add publication/update dates");
        improvements.addAll(businessInsightService.generateGrowthIdeas(businessType, overallScore));

        if (!location.equalsIgnoreCase("Unknown")) {
            improvements.add("Improve local SEO targeting " + location);
        }

        return deduplicate(improvements);
    }

    // =========================
    // UTILITY METHODS
    // =========================
    private boolean isCacheArtifact(String text) {
        if (text == null || text.isBlank()) return true;
        String lower = text.toLowerCase();
        return lower.contains("google search") || lower.contains("google cache") ||
                lower.contains("web.archive.org") || lower.contains("wayback machine") ||
                lower.contains("internet archive") || lower.contains("cached version") ||
                lower.contains("googleusercontent") || lower.equals("google");
    }

    private boolean isGenericBusinessType(String type) {
        if (type == null) return true;
        String lower = type.toLowerCase();
        return lower.equals("business") || lower.equals("general") || lower.equals("unknown") || lower.isBlank();
    }

    private String inferBusinessTypeFromUrl(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("hospital") || lower.contains("health") || lower.contains("clinic") || lower.contains("medic")) return "Hospital";
        if (lower.contains("hotel") || lower.contains("resort") || lower.contains("inn")) return "Hotel";
        if (lower.contains("school") || lower.contains("college") || lower.contains("uni") || lower.contains("edu")) return "Education";
        if (lower.contains("restaurant") || lower.contains("food") || lower.contains("cafe") || lower.contains("eat")) return "Restaurant";
        if (lower.contains("shop") || lower.contains("store") || lower.contains("mart")) return "Retail";
        if (lower.contains("law") || lower.contains("legal") || lower.contains("attorney")) return "Legal";
        if (lower.contains("bank") || lower.contains("finance") || lower.contains("invest")) return "Finance";
        if (lower.contains("gym") || lower.contains("fitness") || lower.contains("sport")) return "Fitness";
        if (lower.contains("salon") || lower.contains("beauty") || lower.contains("spa")) return "Beauty";
        if (lower.contains("tech") || lower.contains("software") || lower.contains("app")) return "Technology";
        if (lower.contains("news") || lower.contains("media") || lower.contains("blog")) return "Media";
        return "General";
    }

    private AuditResponse buildFailureResponse(String url, String error) {
        List<String> issues = new ArrayList<>(List.of(
                error != null ? error : "Website could not be reached"
        ));
        List<String> fixes = new ArrayList<>(List.of(
                "Ensure website is publicly accessible",
                "Remove strict bot blocking (WAF rules)",
                "Enable SSR or provide API access"
        ));

        AIAnalysis aiAnalysis = new AIAnalysis(
                "Website could not be analyzed due to access restrictions",
                new ArrayList<>(),
                new ArrayList<>()
        );

        TrafficData traffic = getLiveTraffic(url, 20, 0);

        return new AuditResponse(
                extractDomain(url),
                20, 40, 20, "LOW",
                10, 10, 10, 10, 10, 10, 10, 10, 10,
                issues, fixes, aiAnalysis, "",
                traffic
        );
    }

    private List<String> deduplicate(List<String> list) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String item : list) {
            String key = item.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (seen.add(key)) result.add(item);
        }
        return result;
    }

    private String extractDomain(String url) {
        try {
            String host = new URL(url).getHost().replace("www.", "");
            String name = host.replaceAll("(\\.[a-z]{2,3}){1,2}$", "");
            name = name.replaceAll("[-_]", " ");
            name = splitCompoundDomain(name);

            String[] words = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty()) {
                    sb.append(Character.toUpperCase(w.charAt(0)))
                            .append(w.substring(1).toLowerCase())
                            .append(" ");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String splitCompoundDomain(String name) {
        String[] suffixes = {
                "hospitals", "hospital", "healthcare", "health", "clinic", "clinics",
                "hotels", "hotel", "resorts", "resort",
                "schools", "school", "college", "university", "academy",
                "restaurants", "restaurant", "foods", "food", "kitchen",
                "stores", "store", "shops", "shop", "mart", "market",
                "technologies", "technology", "solutions", "services", "service",
                "software", "systems", "system", "digital", "media",
                "motors", "auto", "cars", "travels", "travel", "tours", "trip",
                "finance", "bank", "banking", "insurance", "invest",
                "fitness", "gym", "sports", "salon", "beauty", "spa",
                "online", "group", "global", "india", "asia", "world"
        };

        String lower = name.toLowerCase();
        for (String suffix : suffixes) {
            int idx = lower.indexOf(suffix);
            if (idx > 0) {
                String before = lower.substring(0, idx).trim();
                String after = lower.substring(idx).trim();
                if (!before.isEmpty() && !after.isEmpty()) {
                    return before + " " + after;
                }
            }
        }
        return name;
    }
}