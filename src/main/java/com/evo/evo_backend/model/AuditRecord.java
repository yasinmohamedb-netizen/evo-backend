package com.evo.evo_backend.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditRecord {

    private String id;
    private String url;
    private LocalDateTime auditedAt;
    private AuditResponse response;

    public AuditRecord(String url, AuditResponse response) {
        this.id        = UUID.randomUUID().toString();
        this.url       = url;
        this.auditedAt = LocalDateTime.now();
        this.response  = response;
    }

    public String getId()                { return id; }
    public String getUrl()               { return url; }
    public LocalDateTime getAuditedAt()  { return auditedAt; }
    public AuditResponse getResponse()   { return response; }
}