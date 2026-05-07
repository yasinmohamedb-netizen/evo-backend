package com.evo.evo_backend.service.fetch;

import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.service.fetch.PlaywrightFetchService.FetchMethod;

/**
 * Central fetch orchestrator — cascades through 4 strategies automatically:
 *
 * 1. Jsoup (fast, direct HTTP - Upgraded with SSL & Browser Headers)
 * 2. Playwright (headless browser — solves JS-rendered sites)
 * 3. ProxyFetchService (rotating IPs — bypasses WAF/Cloudflare)
 * 4. CacheFetchService (Google Cache → Wayback Machine — last resort)
 */
@Service
public class FetchOrchestrator {

    private static final Logger log = Logger.getLogger(FetchOrchestrator.class.getName());

    private static final int MIN_CONTENT_CHARS = 120;

    @Autowired private PlaywrightFetchService playwrightFetchService;
    @Autowired private ProxyFetchService proxyFetchService;
    @Autowired private CacheFetchService cacheFetchService;

    // =========================
    // MAIN ENTRY POINT
    // =========================
    public FetchOutcome fetch(String url) {

        // ── Layer 1: Jsoup ────────────────────────
        try {
            long start = System.currentTimeMillis();

            // UPGRADED: Added modern headers to prevent SSL fatal alert: internal_error
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,webp,*/*;q=0.8")
                    .timeout(20000) // Increased timeout for slower health directories
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .maxBodySize(0) // Ensure full page content is captured
                    .get();

            long responseTime = System.currentTimeMillis() - start;

            if (hasEnoughContent(doc)) {
                log.info("Layer 1 (Jsoup) success: " + url);
                return FetchOutcome.success(doc, responseTime, FetchMethod.JSOUP, null);
            } else {
                log.info("Jsoup returned thin content/JS shell for: " + url);
            }

        } catch (Exception e) {
            log.info("Jsoup failed: " + e.getMessage());
        }

        // ── Layer 2: Playwright ───────────────────
        // STRATEGIC MOVE: Playwright is now Layer 2 because if Jsoup fails, 
        // the issue is likely Client-Side Rendering (React/Next.js).
        try {
            PlaywrightFetchService.FetchResult pr = playwrightFetchService.fetch(url);

            if (pr.success && pr.doc != null && hasEnoughContent(pr.doc)) {
                log.info("Layer 2 (Playwright) success: " + url);
                return FetchOutcome.success(pr.doc, pr.responseTimeMs, FetchMethod.PLAYWRIGHT, null);
            }

        } catch (Exception e) {
            log.info("Playwright failed: " + e.getMessage());
        }

        // ── Layer 3: Proxy ────────────────────────
        try {
            ProxyFetchService.FetchResult px = proxyFetchService.fetch(url);

            if (px.success && px.doc != null && hasEnoughContent(px.doc)) {
                log.info("Layer 3 (Proxy) success: " + url);
                return FetchOutcome.success(px.doc, px.responseTimeMs, FetchMethod.PROXY, null);
            }

        } catch (Exception e) {
            log.info("Proxy failed: " + e.getMessage());
        }

        // ── Layer 4: Cache ───────────────────────
        try {
            CacheFetchService.CacheFetchResult cr = cacheFetchService.fetch(url);

            if (cr.success && cr.doc != null) {
                log.info("Layer 4 (Cache) success: " + url);
                return FetchOutcome.success(cr.doc, cr.responseTimeMs, cr.method, cr.cacheWarning);
            }

        } catch (Exception e) {
            log.warning("Cache failed: " + e.getMessage());
        }

        // ── All failed ───────────────────────────
        log.warning("All fetch layers failed for: " + url);

        return FetchOutcome.failure(
                "Unable to access website. It may be blocked, behind login, or offline."
        );
    }

    // =========================
    // CONTENT VALIDATION
    // =========================
    private boolean hasEnoughContent(Document doc) {
        if (doc == null) return false;

        String text = doc.text().trim();
        String html = doc.html().toLowerCase();

        // Detect if the page is just a JavaScript container (SPA)
        boolean isJSShell =
                text.length() < MIN_CONTENT_CHARS &&
                (html.contains("id=\"root\"") ||
                 html.contains("id=\"app\"") ||
                 html.contains("__next_data__") ||
                 html.contains("noscript") ||
                 html.contains("enable javascript"));

        return text.length() >= MIN_CONTENT_CHARS && !isJSShell;
    }

    // =========================
    // RESULT WRAPPER
    // =========================
    public static class FetchOutcome {

        public final boolean success;
        public final Document doc;
        public final long responseTimeMs;
        public final FetchMethod method;
        public final String warning;
        public final String error;

        private FetchOutcome(boolean success, Document doc, long responseTimeMs,
                             FetchMethod method, String warning, String error) {
            this.success = success;
            this.doc = doc;
            this.responseTimeMs = responseTimeMs;
            this.method = method;
            this.warning = warning;
            this.error = error;
        }

        public static FetchOutcome success(Document doc, long responseTimeMs,
                                           FetchMethod method, String warning) {
            return new FetchOutcome(true, doc, responseTimeMs, method, warning, null);
        }

        public static FetchOutcome failure(String error) {
            return new FetchOutcome(false, null, 0, null, null, error);
        }

        public boolean isCached() {
            return method == FetchMethod.GOOGLE_CACHE || method == FetchMethod.WAYBACK;
        }
    }
}