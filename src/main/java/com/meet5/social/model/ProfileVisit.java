package com.meet5.social.model;

import java.time.LocalDateTime;

/**
 * Represents a profile visit action
 */
public class ProfileVisit {
    private Long id;
    private Long visitorId;
    private Long visitedId;
    private LocalDateTime visitedAt;
    private transient boolean fraudDetected;
    
    public ProfileVisit() {}
    
    public ProfileVisit(Long visitorId, Long visitedId) {
        this.visitorId = visitorId;
        this.visitedId = visitedId;
        this.visitedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getVisitorId() { return visitorId; }
    public void setVisitorId(Long visitorId) { this.visitorId = visitorId; }
    
    public Long getVisitedId() { return visitedId; }
    public void setVisitedId(Long visitedId) { this.visitedId = visitedId; }
    
    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }

    public boolean isFraudDetected() { return fraudDetected; }
    public void setFraudDetected(boolean fraudDetected) { this.fraudDetected = fraudDetected; }
}
