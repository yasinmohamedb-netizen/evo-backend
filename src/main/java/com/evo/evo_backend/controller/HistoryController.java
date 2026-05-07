package com.evo.evo_backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.AuditRecord;
import com.evo.evo_backend.service.HistoryService;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired 
    private HistoryService historyService;

    // =========================
    // GET ALL HISTORY
    // =========================
    @GetMapping
    public ResponseEntity<List<AuditRecord>> getHistory(@RequestParam String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(historyService.getHistory(url));
    }

    // =========================
    // GET LATEST
    // =========================
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(@RequestParam String url) {
        return historyService.getLatest(url)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // GET TREND
    // =========================
    @GetMapping("/trend")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@RequestParam String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(historyService.getScoreTrend(url));
    }

    // =========================
    // GET ALL URLS
    // =========================
    @GetMapping("/urls")
    public ResponseEntity<List<String>> getAllUrls() {
        return ResponseEntity.ok(historyService.getAllTrackedUrls());
    }

    // =========================
    // DELETE HISTORY
    // =========================
    @DeleteMapping
    public ResponseEntity<Void> clearHistory(@RequestParam String url) {
        historyService.clearHistory(url);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // 🔥 COMPARE (IMPORTANT)
    // =========================
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compare(@RequestParam String url) {

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> result = historyService.compare(url);

        return ResponseEntity.ok(result);
    }
}