package com.evo.evo_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.model.CompetitorResult;
import com.evo.evo_backend.service.CompetitorService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") 
public class CompetitorController {

    @Autowired
    private CompetitorService competitorService;

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@RequestBody Map<String, String> body) {
        String primaryUrl = body.get("primaryUrl");
        String competitorUrl = body.get("competitorUrl");

        // 1. Validation
        if (primaryUrl == null || primaryUrl.isEmpty() || competitorUrl == null || competitorUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("Both primaryUrl and competitorUrl are required.");
        }

        try {
            // 2. Execution
            CompetitorResult result = competitorService.compare(primaryUrl, competitorUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 3. Error Handling (e.g., if one of the sites is down)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Comparison failed: " + e.getMessage());
        }
    }
}