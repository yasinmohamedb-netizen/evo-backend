package com.evo.evo_backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evo.evo_backend.model.AuditRecord;
import com.evo.evo_backend.model.AuditResponse;
import com.evo.evo_backend.model.Fix;
import com.evo.evo_backend.model.Issue;
import com.evo.evo_backend.model.Scan;
import com.evo.evo_backend.model.Scores;
import com.evo.evo_backend.repository.ScanRepository;

@Service
public class HistoryService {

    private static final int MAX_HISTORY_PER_URL = 10;

    // =========================
    // 🔥 In-memory store
    // =========================
    private final Map<String, LinkedList<AuditRecord>> store = new ConcurrentHashMap<>();

    // =========================
    // 🔥 MongoDB
    // =========================
    @Autowired
    private ScanRepository scanRepository;

    // =========================
    // 🔥 AI Progress Engine
    // =========================
    @Autowired
    private AIProgressService aiProgressService;

    // =========================
    // SAVE (Memory + MongoDB)
    // =========================
    public AuditRecord save(String url, AuditResponse response) {

        String normalisedUrl = normalise(url);
        AuditRecord record   = new AuditRecord(normalisedUrl, response);

        // ✅ Update Memory (Real-time tracking)
        store.compute(normalisedUrl, (key, list) -> {
            if (list == null) list = new LinkedList<>();
            list.addFirst(record);

            if (list.size() > MAX_HISTORY_PER_URL) {
                list.removeLast();
            }
            return list;
        });

        // ✅ Update MongoDB (Persistent Journey)
        try {
            Scan scan = convertToScan(response, normalisedUrl);
            scanRepository.save(scan);
            System.out.println("✅ SUCCESSFULLY SAVED TO MONGODB: " + normalisedUrl);
        } catch (Exception e) {
            System.err.println("❌ MONGODB SAVE FAILED: " + e.getMessage());
        }

        return record;
    }

    // =========================
    // CONVERT → MongoDB
    // =========================
    private Scan convertToScan(AuditResponse res, String url) {

        Scan scan = new Scan();
        scan.setUrl(url);

        // Scores
        Scores scores = new Scores();
        scores.setAiVisibility(res.getAiVisibilityScore());
        scores.setContent(res.getContentScore());
        scores.setSchema(res.getSchemaCount());
        scores.setEeat(res.getAiSeoScore());
        scan.setScores(scores);

        // Issues
        if (res.getIssues() != null) {
            List<Issue> issues = res.getIssues().stream()
                    .map(i -> {
                        Issue issue = new Issue();
                        issue.setText(i);
                        issue.setImpact("HIGH");
                        return issue;
                    })
                    .toList();
            scan.setIssues(issues);
        }

        // Fixes
        if (res.getSuggestions() != null) {
            List<Fix> fixes = res.getSuggestions().stream()
                    .map(s -> {
                        Fix fix = new Fix();
                        fix.setText(s);
                        fix.setPriority("HIGH");
                        return fix;
                    })
                    .toList();
            scan.setFixes(fixes);
        }

        // AI Summary
        if (res.getAiAnalysis() != null) {
            scan.setAiSummary(res.getAiAnalysis().toString());
        }

        return scan;
    }

    // =========================
    // GET HISTORY (Memory)
    // =========================
    public List<AuditRecord> getHistory(String url) {
        return Collections.unmodifiableList(
                store.getOrDefault(normalise(url), new LinkedList<>())
        );
    }

    // =========================
    // GET LATEST (Memory)
    // =========================
    public Optional<AuditRecord> getLatest(String url) {
        LinkedList<AuditRecord> list = store.get(normalise(url));
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.getFirst());
    }

    // =========================
    // SCORE TREND (FETCH FROM MONGODB)
    // =========================
    public List<Map<String, Object>> getScoreTrend(String url) {
        List<Scan> scans = scanRepository.findByUrl(normalise(url));
        
        // Formatter to make the chart labels look nice
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm");
    
        return scans.stream().map(scan -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("score", scan.getScores().getAiVisibility());
            
            // Convert MongoDB ObjectId to a readable Date string
            // (MongoDB IDs actually contain the timestamp they were created!)
            java.util.Date date = new java.util.Date(new org.bson.types.ObjectId(scan.getId()).getTimestamp() * 1000L);
            java.time.LocalDateTime ldt = date.toInstant()
                                              .atZone(java.time.ZoneId.systemDefault())
                                              .toLocalDateTime();
            
            point.put("auditedAt", ldt.format(formatter)); 
            point.put("auditId", scan.getId());
            return point;
        }).collect(Collectors.toList());
    }

    // =========================
    // 🔥 COMPARE (Mongo + AI)
    // =========================
    public Map<String, Object> compare(String url) {
        // 1. Normalise to match the Trend Chart's logic
        String normalisedUrl = normalise(url);
        
        // 2. Fetch the top 2
        List<Scan> scans = scanRepository.findTop2ByUrlOrderByIdDesc(normalisedUrl);
    
        // DEBUG: Add this to your console to see what's happening
        System.out.println("Comparing for: " + normalisedUrl + " | Found: " + (scans != null ? scans.size() : 0));
    
        if (scans == null || scans.size() < 2) {
            return Map.of("message", "Not enough data to compare");
        }
    
        Scan current  = scans.get(0);
        Scan previous = scans.get(1);
    
        // Convert and send to AI
        AuditResponse oldRes = convertToAuditResponse(previous);
        AuditResponse newRes = convertToAuditResponse(current);
    
        return aiProgressService.explain(oldRes, newRes);
    }

    // =========================
    // 🔥 Convert Scan → AuditResponse
    // =========================
    private AuditResponse convertToAuditResponse(Scan scan) {

        AuditResponse res = new AuditResponse();

        res.setAiVisibilityScore(scan.getScores().getAiVisibility());
        res.setContentScore(scan.getScores().getContent());
        res.setSchemaCount(scan.getScores().getSchema());
        res.setAiSeoScore(scan.getScores().getEeat());

        // Overall score mapping
        res.setScore(scan.getScores().getAiVisibility());

        return res;
    }

    // =========================
    // GET ALL URLS
    // =========================
    public List<String> getAllTrackedUrls() {
        return new ArrayList<>(store.keySet());
    }

    // =========================
    // DELETE HISTORY
    // =========================
    public void clearHistory(String url) {
        store.remove(normalise(url));
    }

    // =========================
    // NORMALISE URL
    // =========================
    private String normalise(String url) {

        if (url == null) return "";

        url = url.trim().toLowerCase();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }
}