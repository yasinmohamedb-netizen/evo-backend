package com.evo.evo_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.MentionReport;
import com.evo.evo_backend.service.MentionTrackerService;

@RestController
@RequestMapping("/api/mentions")
@CrossOrigin(origins = "*")
public class MentionTrackerController {

    @Autowired private MentionTrackerService mentionTrackerService;

    // =========================
    // 📊 TRACK - POST
    // =========================
    @PostMapping("/track")
    public ResponseEntity<MentionReport> track(
            @RequestBody Map<String, String> body) {

        String url = body.get("url");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MentionReport report = mentionTrackerService.track(url);
        return ResponseEntity.ok(report);
    }

    // =========================
    // 📊 TRACK - GET
    // =========================
    @GetMapping("/track")
    public ResponseEntity<MentionReport> trackGet(
            @RequestParam String url) {

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MentionReport report = mentionTrackerService.track(url);
        return ResponseEntity.ok(report);
    }
}