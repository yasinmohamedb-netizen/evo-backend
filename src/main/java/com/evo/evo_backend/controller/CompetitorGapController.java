package com.evo.evo_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.CompetitorGapAnalysisRequest;
import com.evo.evo_backend.model.CompetitorGapAnalysisResponse;
import com.evo.evo_backend.service.CompetitorGapService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Standardized
public class CompetitorGapController {

    @Autowired
    private CompetitorGapService service;

    @PostMapping("/competitor-gap")
    public ResponseEntity<CompetitorGapAnalysisResponse> analyze(
            @RequestBody CompetitorGapAnalysisRequest request) {
        
        // Quick validation check
        if (request.getYourUrl() == null || request.getCompetitorUrls() == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(service.analyze(request));
    }
}