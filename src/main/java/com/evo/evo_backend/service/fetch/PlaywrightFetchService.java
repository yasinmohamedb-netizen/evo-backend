package com.evo.evo_backend.service.fetch;

import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

@Service
public class PlaywrightFetchService {

    private static final Logger log = Logger.getLogger(PlaywrightFetchService.class.getName());
    private static final int TIMEOUT_MS = 25000;
    private static final int MIN_CONTENT_CHARS = 120;

    public FetchResult fetch(String url) {
        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            Page page = browser.newPage();

            long start = System.currentTimeMillis();

            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            long responseTime = System.currentTimeMillis() - start;

            String html = page.content();
            Document doc = Jsoup.parse(html, url);

            boolean hasContent = doc.text().length() >= MIN_CONTENT_CHARS;

            return new FetchResult(doc, responseTime, hasContent, FetchMethod.PLAYWRIGHT, null);

        } catch (Exception e) {
            return new FetchResult(null, 0, false, FetchMethod.PLAYWRIGHT, e.getMessage());
        }
    }

    public static class FetchResult {
        public final Document doc;
        public final long responseTimeMs;
        public final boolean success;
        public final FetchMethod method;
        public final String errorMessage;

        public FetchResult(Document doc, long responseTimeMs, boolean success,
                           FetchMethod method, String errorMessage) {
            this.doc = doc;
            this.responseTimeMs = responseTimeMs;
            this.success = success;
            this.method = method;
            this.errorMessage = errorMessage;
        }
    }

    public enum FetchMethod {
        JSOUP, PLAYWRIGHT, PROXY, GOOGLE_CACHE, WAYBACK
    }
}