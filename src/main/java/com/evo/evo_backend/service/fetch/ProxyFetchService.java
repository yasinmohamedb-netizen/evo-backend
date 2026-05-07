package com.evo.evo_backend.service.fetch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.service.fetch.PlaywrightFetchService.FetchMethod;

@Service
public class ProxyFetchService {

    private static final Logger log = Logger.getLogger(ProxyFetchService.class.getName());
    private static final int TIMEOUT_MS = 30000;
    private static final int MIN_CONTENT_CHARS = 120;

    @Value("${proxy.scraperapi.key:}")
    private String apiKey;

    @Value("${proxy.enabled:false}")
    private boolean enabled;

    public FetchResult fetch(String targetUrl) {

        if (!enabled || apiKey.isBlank()) {
            return new FetchResult(null, 0, false, FetchMethod.PROXY, "Proxy disabled");
        }

        try {
            String encoded = URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
            String proxyUrl = "https://api.scraperapi.com/?api_key=" + apiKey + "&url=" + encoded;

            long start = System.currentTimeMillis();

            Document doc = Jsoup.connect(proxyUrl)
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .get();

            long responseTime = System.currentTimeMillis() - start;

            boolean hasContent = doc.text().length() >= MIN_CONTENT_CHARS;

            return new FetchResult(doc, responseTime, hasContent, FetchMethod.PROXY, null);

        } catch (Exception e) {
            return new FetchResult(null, 0, false, FetchMethod.PROXY, e.getMessage());
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
}