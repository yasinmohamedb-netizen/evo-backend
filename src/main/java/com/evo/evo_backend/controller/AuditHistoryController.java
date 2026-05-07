package com.evo.evo_backend.controller;

import com.evo.evo_backend.model.AuditHistory;
import com.evo.evo_backend.repository.AuditHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "http://localhost:3000") // Allows your React app to call this
public class AuditHistoryController {

    @Autowired
    private AuditHistoryRepository repository;

    @GetMapping("/{url}")
    public List<AuditHistory> getHistory(@PathVariable String url) {
        // This assumes you have a custom method in your repository 
        // or you can use repository.findAll() and filter
        return repository.findByUrlOrderByTimestampDesc(url);
    }
}