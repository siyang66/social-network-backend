package com.meet5.social.model;

import java.time.LocalDateTime;

public class FraudRecord {
    private Long id;
    private Long userId;
    private long visitCount;
    private long likeCount;
    private int timeWindowMinutes;
    private LocalDateTime detectedAt;

    public FraudRecord(Long userId, long visitCount, long likeCount, int timeWindowMinutes) {
        this.userId = userId;
        this.visitCount = visitCount;
        this.likeCount = likeCount;
        this.timeWindowMinutes = timeWindowMinutes;
        this.detectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public long getVisitCount() { return visitCount; }
    public long getLikeCount() { return likeCount; }
    public int getTimeWindowMinutes() { return timeWindowMinutes; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
}
