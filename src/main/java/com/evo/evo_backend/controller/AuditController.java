package com.evo.evo_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.service.AuditService;
import com.evo.evo_backend.service.HistoryService;
import com.evo.evo_backend.service.ReportService;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired 
    private AuditService auditService;

    @Autowired 
    private HistoryService historyService;

    @Autowired 
    private ReportService reportService;

    // ============================================================
    // POST ANALYZE
    // ============================================================
    @PostMapping
    public ResponseEntity<AuditResponse> audit(@RequestBody Map<String, String> body) {
        String input = body.get("url");
        String location = body.getOrDefault("location", "India");

        AuditResponse response = processInput(input, location);
        historyService.save(input, response);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GET ANALYZE
    // ============================================================
    @GetMapping
    public ResponseEntity<AuditResponse> auditGet(
            @RequestParam String url, 
            @RequestParam(required = false, defaultValue = "India") String location) {

        AuditResponse response = processInput(url, location);
        historyService.save(url, response);

        return ResponseEntity.ok(response);
    }

    // ============================================================
    // TREND API
    // ============================================================
    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@RequestParam String url) {
        List<Map<String, Object>> trend = historyService.getScoreTrend(url);
        
        if (trend.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(trend);
    }

    // ============================================================
    // COMPARE API
    // ============================================================
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compare(@RequestParam String url) {
        Map<String, Object> result = historyService.compare(url);
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // DOWNLOAD PDF
    // ============================================================
    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam String url,
            @RequestParam(required = false, defaultValue = "India") String location) {

        AuditResponse response = processInput(url, location);
        byte[] pdf = reportService.generateAuditPdf(response, url);

        String filename = isUrl(url) ? "seo-audit.pdf" : "market-strategy.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ============================================================
    // PRIVATE HELPER: Routing & Traffic Injection Logic
    // ============================================================
    private AuditResponse processInput(String input, String location) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Search input cannot be empty");
        }

        AuditResponse response;
        if (isUrl(input)) {
            // 1. Perform technical SEO analysis
            response = auditService.analyze(input);
            
            // 2. Extract real metrics from the analysis to feed the traffic generator
            int score = response.getScore();
            // Use word count or internal link count as a proxy for authority
            int linkCount = response.getInternalLinks() != 0 ? response.getInternalLinks() : 5;
            
            // 3. Inject Dynamic Live Traffic Analysis
            // This ensures google.com gets high traffic and tiny-site.com gets low traffic
            response.setTraffic(auditService.getLiveTraffic(input, score, linkCount)); 
            
        } else {
            response = auditService.discoverKeywords(input, location);
        }
        return response;
    }

    private boolean isUrl(String input) {
        return input.trim().matches("^(http|https)://.*") || input.contains(".");
    }
}