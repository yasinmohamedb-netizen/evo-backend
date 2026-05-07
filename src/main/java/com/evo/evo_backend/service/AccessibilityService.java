package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.ServiceResult;

@Service
public class AccessibilityService {

    public ServiceResult analyze(Document doc) {

        List<String> issues = new ArrayList<>();
        List<String> highPriority = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority = new ArrayList<>();

        int score = 100;

        // ================================
        // 🏷️ LANDMARK ELEMENTS
        // ================================
        boolean hasMain = !doc.select("main").isEmpty();
        boolean hasHeader = !doc.select("header").isEmpty();
        boolean hasNav = !doc.select("nav").isEmpty();
        boolean hasFooter = !doc.select("footer").isEmpty();

        int landmarkCount = (hasMain ? 1 : 0) + (hasHeader ? 1 : 0)
                + (hasNav ? 1 : 0) + (hasFooter ? 1 : 0);

        if (landmarkCount < 2) {
            score -= 20;
            issues.add("Missing semantic landmark elements (main/header/nav/footer)");
            highPriority.add("Add semantic HTML5 landmarks: <main>, <header>, <nav>, <footer>");
        } else if (landmarkCount < 4) {
            score -= 8;
            issues.add("Some landmark elements missing");
            mediumPriority.add("Complete semantic landmark structure");
        }

        // ================================
        // 🔘 BUTTON VS DIV ONCLICK
        // ================================
        int divOnclick = doc.select("div[onclick], span[onclick]").size();
        if (divOnclick > 0) {
            score -= 15;
            issues.add("Clickable divs/spans instead of <button> (" + divOnclick + " found)");
            highPriority.add("Replace div[onclick] with semantic <button> elements");
        }

        // ================================
        // 🖼️ ARIA LABELS
        // ================================
        int buttons = doc.select("button").size();
        int buttonsWithoutLabel = 0;

        for (Element btn : doc.select("button")) {
            boolean hasText = !btn.text().trim().isEmpty();
            boolean hasAriaLabel = btn.hasAttr("aria-label");
            boolean hasAriaLabelledBy = btn.hasAttr("aria-labelledby");
            boolean hasTitle = btn.hasAttr("title");
            if (!hasText && !hasAriaLabel && !hasAriaLabelledBy && !hasTitle) {
                buttonsWithoutLabel++;
            }
        }

        if (buttonsWithoutLabel > 0) {
            score -= 10;
            issues.add(buttonsWithoutLabel + " button(s) missing accessible labels");
            mediumPriority.add("Add aria-label to icon-only buttons");
        }

        // ================================
        // 📝 FORM LABELS
        // ================================
        int inputs = doc.select("input:not([type=hidden]):not([type=submit]):not([type=button])").size();
        int inputsWithLabel = doc.select("label").size();

        if (inputs > 0 && inputsWithLabel < inputs) {
            int unlabeled = inputs - inputsWithLabel;
            score -= 10;
            issues.add(unlabeled + " form input(s) may lack associated labels");
            mediumPriority.add("Associate <label> elements with all form inputs");
        }

        // ================================
        // 🔤 LANGUAGE ATTRIBUTE
        // ================================
        String lang = doc.select("html").attr("lang");
        if (lang == null || lang.trim().isEmpty()) {
            score -= 10;
            issues.add("Missing lang attribute on <html> element");
            mediumPriority.add("Add lang attribute to <html> (e.g., lang=\"en\")");
        }

        // ================================
        // 📰 HEADING ORDER
        // ================================
        boolean hasH1 = !doc.select("h1").isEmpty();
        int h1Count = doc.select("h1").size();

        if (!hasH1) {
            score -= 10;
            issues.add("No <h1> tag found on page");
            mediumPriority.add("Add a single descriptive <h1> heading");
        } else if (h1Count > 1) {
            score -= 5;
            issues.add("Multiple <h1> tags found (" + h1Count + ") — only one recommended");
            lowPriority.add("Use a single <h1> per page");
        }

        // ================================
        // 🔗 LINKS WITH MEANINGFUL TEXT
        // ================================
        int vagueLinks = 0;
        for (Element a : doc.select("a")) {
            String text = a.text().trim().toLowerCase();
            if (text.equals("click here") || text.equals("read more")
                    || text.equals("here") || text.equals("more") || text.isEmpty()) {
                vagueLinks++;
            }
        }

        if (vagueLinks > 3) {
            score -= 10;
            issues.add("Multiple links with vague text (" + vagueLinks + " found: 'click here', 'read more', etc.)");
            mediumPriority.add("Use descriptive link text instead of 'click here' or 'read more'");
        } else if (vagueLinks > 0) {
            score -= 5;
            issues.add(vagueLinks + " link(s) with vague anchor text");
            lowPriority.add("Improve link text to be more descriptive");
        }

        score = Math.max(0, Math.min(score, 100));

        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        // 🔥 FIXED: Added empty string as the 5th argument to match the new constructor
        return new ServiceResult("accessibility", score, issues, fixes, "");
    }
}