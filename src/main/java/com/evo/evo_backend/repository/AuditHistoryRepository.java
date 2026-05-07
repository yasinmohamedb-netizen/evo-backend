package com.evo.evo_backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.evo.evo_backend.model.AuditHistory;

@Repository
public interface AuditHistoryRepository extends MongoRepository<AuditHistory, String> {
    
    // This method finds all audits for a specific URL and sorts them by newest first
    List<AuditHistory> findByUrlOrderByTimestampDesc(String url);
}