package com.evo.evo_backend.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class LinkHealthService {

    // Max links to actively probe (avoid long timeouts)
    private static final int MAX_PROBE_LINKS = 15;

    public ServiceResult analyze(Document doc, String baseUrl) {

        List<String> issues = new ArrayList<>();
        List<String> highPriority = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority = new ArrayList<>();

        int score = 100;
        String origin = extractOrigin(baseUrl);

        List<String> allHrefs = new ArrayList<>();
        List<String> internalLinks = new ArrayList<>();
        List<String> externalLinks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // ================================
        // 🔗 COLLECT ALL LINKS
        // ================================
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("abs:href").trim();
            if (href.isEmpty() || href.startsWith("mailto:") || href.startsWith("tel:") || href.startsWith("javascript:")) {
                continue;
            }
            if (seen.contains(href)) continue;
            seen.add(href);

            allHrefs.add(href);

            if (href.startsWith(origin)) {
                internalLinks.add(href);
            } else {
                externalLinks.add(href);
            }
        }

        // ================================
        // 📊 LINK COUNTS
        // ================================
        if (allHrefs.isEmpty()) {
            score -= 20;
            issues.add("No outbound links found on the page");
            highPriority.add("Add relevant internal and external links to improve knowledge graph signals");
        } else {
            if (internalLinks.isEmpty()) {
                score -= 15;
                issues.add("No internal links found — poor site navigation for crawlers");
                highPriority.add("Add internal links to key pages (home, about, services, blog)");
            } else if (internalLinks.size() < 3) {
                score -= 8;
                issues.add("Very few internal links (" + internalLinks.size() + ")");
                mediumPriority.add("Add more internal links to improve crawlability");
            }

            if (externalLinks.isEmpty()) {
                score -= 5;
                issues.add("No external links found — may appear isolated to AI crawlers");
                lowPriority.add("Add relevant external links to authoritative sources");
            }
        }

        // ================================
        // 🔍 ANCHOR TEXT QUALITY
        // ================================
        int vagueAnchors = 0;
        for (Element a : doc.select("a[href]")) {
            String text = a.text().trim().toLowerCase();
            if (text.equals("click here") || text.equals("read more") || text.equals("here")
                    || text.equals("more") || text.equals("link") || text.isEmpty()) {
                vagueAnchors++;
            }
        }

        if (vagueAnchors > 5) {
            score -= 10;
            issues.add("Many links with non-descriptive anchor text (" + vagueAnchors + ")");
            mediumPriority.add("Replace vague link text ('click here', 'read more') with descriptive keywords");
        } else if (vagueAnchors > 0) {
            score -= 5;
            issues.add(vagueAnchors + " link(s) with vague anchor text");
            lowPriority.add("Improve anchor text to be keyword-descriptive");
        }

        // ================================
        // 💔 BROKEN LINK DETECTION (sampled)
        // ================================
        List<String> brokenLinks = new ArrayList<>();
        List<String> linksToProbe = new ArrayList<>();

        // Probe internal links first (more critical), then external
        linksToProbe.addAll(internalLinks);
        linksToProbe.addAll(externalLinks);

        int probed = 0;
        for (String href : linksToProbe) {
            if (probed >= MAX_PROBE_LINKS) break;
            try {
                int statusCode = Jsoup.connect(href)
                        .userAgent("Mozilla/5.0")
                        .timeout(5000)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .followRedirects(true)
                        .execute()
                        .statusCode();

                if (statusCode == 404 || statusCode == 410 || statusCode == 500) {
                    brokenLinks.add(href + " [" + statusCode + "]");
                }
                probed++;
            } catch (Exception ignored) {
                // Timeout or network error — skip silently
                probed++;
            }
        }

        if (!brokenLinks.isEmpty()) {
            int deduction = Math.min(brokenLinks.size() * 8, 30);
            score -= deduction;
            issues.add(brokenLinks.size() + " broken link(s) detected");
            highPriority.add("Fix broken links: " + String.join(", ", brokenLinks.subList(0, Math.min(3, brokenLinks.size()))));
        }

        // ================================
        // 🔁 NOFOLLOW OVERUSE
        // ================================
        int nofollowCount = doc.select("a[rel~=nofollow]").size();
        int totalLinks = doc.select("a[href]").size();

        if (totalLinks > 0) {
            double nofollowRatio = (double) nofollowCount / totalLinks;
            if (nofollowRatio > 0.7) {
                score -= 10;
                issues.add("Over 70% of links have rel=nofollow (" + nofollowCount + "/" + totalLinks + ")");
                mediumPriority.add("Remove unnecessary nofollow from internal navigation links");
            }
        }

        score = Math.max(0, Math.min(score, 100));

        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        return new ServiceResult("linkHealth", score, issues, fixes);
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