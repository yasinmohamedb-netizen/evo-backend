package com.evo.evo_backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.evo.evo_backend.model.Scan;

@Repository
public interface ScanRepository extends MongoRepository<Scan, String> {

    // Used by getScoreTrend to show the full history on the chart
    List<Scan> findByUrl(String url);

    // Used by compare to get just the latest two results
    List<Scan> findTop2ByUrlOrderByIdDesc(String url);
}