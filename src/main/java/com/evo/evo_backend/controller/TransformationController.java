package com.evo.evo_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evo.evo_backend.service.TransformationService;

@RestController
@RequestMapping("/api/transform")
@CrossOrigin(origins = "*")
public class TransformationController {

    @Autowired
    private TransformationService transformationService;

    @PostMapping("/bridge")
    public ResponseEntity<Map<String, String>> getBridge(@RequestBody Map<String, String> request) {
        String myContent = request.get("myContent");
        String competitorContent = request.get("competitorContent");

        if (myContent == null || competitorContent == null) {
            return ResponseEntity.badRequest().build();
        }

        String bridgeContent = transformationService.generateContentBridge(myContent, competitorContent);
        
        // Return as a JSON object
        return ResponseEntity.ok(Map.of("bridgeContent", bridgeContent));
    }
}