package com.evo.evo_backend.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class EEATService {

    public ServiceResult analyze(Document doc, String url) {

        List<String> issues = new ArrayList<>();
        List<String> fixes = new ArrayList<>();
        int score = 100;

        String text = doc.text().toLowerCase();
        String origin = extractOrigin(url);

        // ================================
        // 🎯 EXPERIENCE
        // ================================
        int experienceScore = 100;

        boolean hasCaseStudies = text.contains("case study") || 
            text.contains("case studies") ||
            text.contains("success stor");

        boolean hasTestimonials = text.contains("testimonial") ||
            text.contains("review") ||
            !doc.select("[class*=testimonial],[class*=review]").isEmpty();

        boolean hasPortfolio = text.contains("portfolio") || 
            text.contains("our work") ||
            text.contains("projects");

        if (!hasCaseStudies && !hasTestimonials && !hasPortfolio) {
            experienceScore -= 40;
            issues.add("No experience signals found (case studies/testimonials)");
            fixes.add("Add case studies or client testimonials");
        }

        // ================================
        // 🎓 EXPERTISE
        // ================================
        int expertiseScore = 100;

        boolean hasAuthorBio = !doc.select(
            "[class*=author],[rel=author],address"
        ).isEmpty() || text.contains("written by") || 
         text.contains("author:");

        boolean hasCredentials = text.contains("certified") ||
            text.contains("expert") ||
            text.contains("years of experience") ||
            text.contains("phd") ||
            text.contains("degree");

        boolean hasAboutPage = false;
        try {
            var aboutLink = doc.select("a[href*=about]").first();
            hasAboutPage = aboutLink != null;
        } catch (Exception ignored) {}

        if (!hasAuthorBio) {
            expertiseScore -= 30;
            issues.add("No author bio found");
            fixes.add("Add author bios with credentials");
        }

        if (!hasCredentials) {
            expertiseScore -= 20;
            issues.add("No expertise credentials");
            fixes.add("Mention certifications and experience");
        }

        if (!hasAboutPage) {
            expertiseScore -= 15;
            issues.add("No About page link");
            fixes.add("Add About page");
        }

        // ================================
        // 🏆 AUTHORITATIVENESS
        // ================================
        int authorityScore = 100;

        boolean hasPress = text.contains("featured in") ||
            text.contains("as seen in") ||
            text.contains("press") ||
            !doc.select("[class*=press],[class*=media]").isEmpty();

        boolean hasStats = doc.text().matches(".*\\d+[k+]?\\s+" +
            "(customers|clients|users|companies).*");

        if (!hasPress) {
            authorityScore -= 25;
            issues.add("No press mentions");
            fixes.add("Add media/press section");
        }

        if (!hasStats) {
            authorityScore -= 20;
            issues.add("No social proof stats");
            fixes.add("Add metrics like users, clients");
        }

        // ================================
        // 🔒 TRUST
        // ================================
        int trustScore = 100;

        boolean hasHTTPS = url.startsWith("https://");
        boolean hasPrivacy = !doc.select("a[href*=privacy]").isEmpty();
        boolean hasTerms = !doc.select("a[href*=terms]").isEmpty();
        boolean hasContact = !doc.select("a[href*=contact]").isEmpty();

        if (!hasHTTPS) {
            trustScore -= 30;
            issues.add("Not using HTTPS");
            fixes.add("Enable HTTPS");
        }

        if (!hasPrivacy) {
            trustScore -= 20;
            issues.add("No privacy policy");
            fixes.add("Add privacy policy");
        }

        if (!hasTerms) {
            trustScore -= 10;
            issues.add("No terms page");
            fixes.add("Add terms page");
        }

        if (!hasContact) {
            trustScore -= 15;
            issues.add("No contact page");
            fixes.add("Add contact page");
        }

        // ================================
        // FINAL SCORE
        // ================================
        int eeatScore = (int) Math.round(
            experienceScore * 0.20 +
            expertiseScore  * 0.30 +
            authorityScore  * 0.25 +
            trustScore      * 0.25
        );

        return new ServiceResult(
            "EEAT",
            Math.max(0, Math.min(eeatScore, 100)),
            issues,
            fixes
        );
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