package com.meet5.social.model;

import java.time.LocalDateTime;

/**
 * Represents a visitor with their profile information and visit time
 */
public class VisitorInfo {
    private Long id;
    private String username;
    private String name;
    private Integer age;
    private LocalDateTime visitTime;

    public VisitorInfo() {}

    public VisitorInfo(Long id, String username, String name, Integer age, LocalDateTime visitTime) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.age = age;
        this.visitTime = visitTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
}
