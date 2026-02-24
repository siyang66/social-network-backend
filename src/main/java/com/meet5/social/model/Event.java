package com.meet5.social.model;

import java.time.LocalDateTime;

public class Event {
    private Long id;
    private LocalDateTime eventTime;
    private String location;
    private String description;
    private Long organizerId;

    public Event() {}

    public Event(LocalDateTime eventTime, String location, String description, Long organizerId) {
        this.eventTime = eventTime;
        this.location = location;
        this.description = description;
        this.organizerId = organizerId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
}
