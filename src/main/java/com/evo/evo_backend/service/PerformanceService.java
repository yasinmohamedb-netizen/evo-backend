package com.evo.evo_backend.service;

import com.evo.evo_backend.model.ServiceResult;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceService {

    public ServiceResult analyze(Document doc, long responseTimeMs) {

        List<String> issues = new ArrayList<>();
        List<String> highPriority = new ArrayList<>();
        List<String> mediumPriority = new ArrayList<>();
        List<String> lowPriority = new ArrayList<>();

        int score = 100;

        // ================================
        // ⏱️ RESPONSE TIME
        // ================================
        if (responseTimeMs > 5000) {
            score -= 25;
            issues.add("Very slow page response (" + responseTimeMs + "ms)");
            highPriority.add("Optimize server response time (target < 2s)");
        } else if (responseTimeMs > 3000) {
            score -= 15;
            issues.add("Slow page response (" + responseTimeMs + "ms)");
            mediumPriority.add("Improve server response time");
        } else if (responseTimeMs > 1500) {
            score -= 5;
            issues.add("Moderate page response (" + responseTimeMs + "ms)");
            lowPriority.add("Consider caching or CDN for faster response");
        }

        // ================================
        // 🖼️ IMAGE ALT TAGS
        // ================================
        int totalImages = doc.select("img").size();
        int missingAlt = 0;

        for (Element img : doc.select("img")) {
            String alt = img.attr("alt");
            if (alt == null || alt.trim().isEmpty()) {
                missingAlt++;
            }
        }

        if (totalImages > 0) {
            double missingRatio = (double) missingAlt / totalImages;
            if (missingRatio > 0.5) {
                score -= 15;
                issues.add("Over 50% of images missing alt text (" + missingAlt + "/" + totalImages + ")");
                highPriority.add("Add descriptive alt text to all images");
            } else if (missingAlt > 0) {
                score -= 8;
                issues.add(missingAlt + " image(s) missing alt text");
                mediumPriority.add("Add alt text to remaining images");
            }
        }

        // ================================
        // 🚧 RENDER-BLOCKING RESOURCES
        // ================================
        int blockingScripts = 0;
        for (Element script : doc.select("script[src]")) {
            boolean isDeferred = script.hasAttr("defer");
            boolean isAsync = script.hasAttr("async");
            boolean isModule = "module".equals(script.attr("type"));
            if (!isDeferred && !isAsync && !isModule) {
                blockingScripts++;
            }
        }

        if (blockingScripts > 5) {
            score -= 15;
            issues.add("Many render-blocking scripts detected (" + blockingScripts + ")");
            highPriority.add("Add defer/async to non-critical scripts");
        } else if (blockingScripts > 2) {
            score -= 8;
            issues.add(blockingScripts + " render-blocking script(s) found");
            mediumPriority.add("Use defer/async on scripts where possible");
        }

        // ================================
        // 📦 INLINE STYLES (heavy pages)
        // ================================
        int inlineStyleElements = doc.select("[style]").size();
        if (inlineStyleElements > 30) {
            score -= 5;
            issues.add("Excessive inline styles (" + inlineStyleElements + " elements)");
            lowPriority.add("Move inline styles to external CSS");
        }

        // ================================
        // 🔗 EXTERNAL CSS COUNT
        // ================================
        int cssCount = doc.select("link[rel=stylesheet]").size();
        if (cssCount > 8) {
            score -= 5;
            issues.add("Too many external CSS files (" + cssCount + ")");
            lowPriority.add("Bundle CSS files to reduce HTTP requests");
        }

        score = Math.max(0, Math.min(score, 100));

        List<String> fixes = new ArrayList<>();
        fixes.addAll(highPriority);
        fixes.addAll(mediumPriority);
        fixes.addAll(lowPriority);

        return new ServiceResult("performance", score, issues, fixes);
    }
}