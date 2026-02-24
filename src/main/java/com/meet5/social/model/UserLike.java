package com.meet5.social.model;

import java.time.LocalDateTime;

/**
 * Represents a user like action
 */
public class UserLike {
    private Long id;
    private Long likerId;
    private Long likedId;
    private LocalDateTime likedAt;
    private transient boolean fraudDetected;
    
    public UserLike() {}
    
    public UserLike(Long likerId, Long likedId) {
        this.likerId = likerId;
        this.likedId = likedId;
        this.likedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getLikerId() { return likerId; }
    public void setLikerId(Long likerId) { this.likerId = likerId; }
    
    public Long getLikedId() { return likedId; }
    public void setLikedId(Long likedId) { this.likedId = likedId; }
    
    public LocalDateTime getLikedAt() { return likedAt; }
    public void setLikedAt(LocalDateTime likedAt) { this.likedAt = likedAt; }

    public boolean isFraudDetected() { return fraudDetected; }
    public void setFraudDetected(boolean fraudDetected) { this.fraudDetected = fraudDetected; }
}
