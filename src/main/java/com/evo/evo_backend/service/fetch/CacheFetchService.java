package com.evo.evo_backend.service.fetch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.service.fetch.PlaywrightFetchService.FetchMethod;

/**
 * Last-resort fetcher. When a site is completely unreachable (blocked by WAF,
 * behind auth, or offline), tries to fetch a cached version:
 *
 *  1. Google Cache
 *  2. Wayback Machine
 */
@Service
public class CacheFetchService {

    private static final Logger log = Logger.getLogger(CacheFetchService.class.getName());
    private static final int TIMEOUT_MS = 15000;
    private static final int MIN_CONTENT_CHARS = 120;

    // =========================
    // MAIN ENTRY
    // =========================
    public CacheFetchResult fetch(String targetUrl) {

        CacheFetchResult google = fetchGoogleCache(targetUrl);
        if (google.success) return google;

        return fetchWayback(targetUrl);
    }

    // =========================
    // GOOGLE CACHE
    // =========================
    private CacheFetchResult fetchGoogleCache(String targetUrl) {
        try {
            String cacheUrl = "https://webcache.googleusercontent.com/search?q=cache:"
                    + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

            long start = System.currentTimeMillis();

            Document doc = Jsoup.connect(cacheUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            long responseTime = System.currentTimeMillis() - start;

            if (doc.text().length() < MIN_CONTENT_CHARS) {
                return new CacheFetchResult(null, 0, false, FetchMethod.GOOGLE_CACHE, null, null);
            }

            doc.select("#google-cache-hdr, .c-tl, #google-cache-ccl").remove();

            String date = extractGoogleCacheDate(doc);

            return new CacheFetchResult(
                    doc,
                    responseTime,
                    true,
                    FetchMethod.GOOGLE_CACHE,
                    date,
                    "Using Google Cache"
            );

        } catch (Exception e) {
            return new CacheFetchResult(null, 0, false, FetchMethod.GOOGLE_CACHE, null, null);
        }
    }

    // =========================
    // WAYBACK
    // =========================
    private CacheFetchResult fetchWayback(String targetUrl) {
        try {
            String cdxUrl = "https://archive.org/wayback/available?url="
                    + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

            String response = Jsoup.connect(cdxUrl)
                    .ignoreContentType(true)
                    .get()
                    .body()
                    .text();

            String snapshot = extractWaybackUrl(response);
            if (snapshot == null) {
                return new CacheFetchResult(null, 0, false, FetchMethod.WAYBACK, null, null);
            }

            long start = System.currentTimeMillis();

            Document doc = Jsoup.connect(snapshot)
                    .userAgent("Mozilla/5.0")
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            long responseTime = System.currentTimeMillis() - start;

            if (doc.text().length() < MIN_CONTENT_CHARS) {
                return new CacheFetchResult(null, 0, false, FetchMethod.WAYBACK, null, null);
            }

            doc.select("#wm-ipp").remove();

            String date = extractWaybackDate(snapshot);

            return new CacheFetchResult(
                    doc,
                    responseTime,
                    true,
                    FetchMethod.WAYBACK,
                    date,
                    "Using Wayback snapshot"
            );

        } catch (Exception e) {
            return new CacheFetchResult(null, 0, false, FetchMethod.WAYBACK, null, null);
        }
    }

    // =========================
    // HELPERS
    // =========================
    private String extractGoogleCacheDate(Document doc) {
        try {
            String text = doc.text();
            int idx = text.indexOf("appeared on ");
            if (idx >= 0) {
                return text.substring(idx + 12, Math.min(idx + 32, text.length())).trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractWaybackUrl(String json) {
        try {
            int idx = json.indexOf("\"url\":\"");
            if (idx < 0) return null;

            int start = idx + 7;
            int end = json.indexOf("\"", start);

            return json.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractWaybackDate(String url) {
        try {
            int idx = url.indexOf("/web/");
            String date = url.substring(idx + 5, idx + 13);

            return date.substring(6, 8) + "/" +
                   date.substring(4, 6) + "/" +
                   date.substring(0, 4);

        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // RESULT CLASS
    // =========================
    public static class CacheFetchResult {
        public final Document doc;
        public final long responseTimeMs;
        public final boolean success;
        public final FetchMethod method;
        public final String cachedDate;
        public final String cacheWarning;

        public CacheFetchResult(Document doc, long responseTimeMs, boolean success,
                                FetchMethod method, String cachedDate, String cacheWarning) {
            this.doc = doc;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.method = method;
            this.cachedDate = cachedDate;
            this.cacheWarning = cacheWarning;
        }
    }
}