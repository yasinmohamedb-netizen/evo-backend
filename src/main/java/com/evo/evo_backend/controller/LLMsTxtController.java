package com.evo.evo_backend.controller;

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

import com.evo.evo_backend.service.LLMsTxtGeneratorService;

@RestController
@RequestMapping("/api/llms-txt")
@CrossOrigin(origins = "*")
public class LLMsTxtController {

    @Autowired private LLMsTxtGeneratorService llmsTxtGeneratorService;

    // =========================
    // 📄 GENERATE - POST
    // returns plain text content
    // =========================
    @PostMapping("/generate")
    public ResponseEntity<String> generate(@RequestBody Map<String, String> body) {

        String url = body.get("url");

        if (url == null || url.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("# Error: URL is required");
        }

        String content = llmsTxtGeneratorService.generate(url);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    // =========================
    // 📄 GENERATE - GET
    // same but via query param
    // =========================
    @GetMapping("/generate")
    public ResponseEntity<String> generateGet(@RequestParam String url) {

        if (url == null || url.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("# Error: URL is required");
        }

        String content = llmsTxtGeneratorService.generate(url);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    // =========================
    // 📥 DOWNLOAD - as file
    // =========================
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String url) {

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String content = llmsTxtGeneratorService.generate(url);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=llms.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes());
    }
}