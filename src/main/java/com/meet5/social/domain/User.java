package com.meet5.social.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * User Domain Model
 * Represents a user profile with validation and data integrity
 */
public class User {
    
    private Long id;
    private String username;
    private String email;
    private String name;
    private Integer age;
    private Boolean isFraud;
    private Map<String, Object> customFields;
    
    // Constructors
    public User() {
        this.customFields = new HashMap<>();
        this.isFraud = false;
    }
    
    public User(String username, String email, String name, Integer age) {
        this();
        this.username = username;
        this.email = email;
        this.name = name;
        this.age = age;
    }
    
    // Validation Methods
    public void validate() throws ValidationException {
        validateUsername();
        validateEmail();
        validateName();
        validateAge();
    }
    
    private void validateUsername() throws ValidationException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new ValidationException("Username must be between 3 and 50 characters");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new ValidationException("Username can only contain letters, numbers, and underscores");
        }
    }
    
    private void validateEmail() throws ValidationException {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }
    
    private void validateName() throws ValidationException {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        if (name.length() > 100) {
            throw new ValidationException("Name must not exceed 100 characters");
        }
    }
    
    private void validateAge() throws ValidationException {
        if (age == null) {
            throw new ValidationException("Age is required");
        }
        if (age < 18 || age > 150) {
            throw new ValidationException("Age must be between 18 and 150");
        }
    }
    
    // Custom Fields Management
    public void addCustomField(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new ValidationException("Custom field key cannot be empty");
        }
        this.customFields.put(key, value);
    }
    
    public Object getCustomField(String key) {
        return this.customFields.get(key);
    }
    
    public Map<String, Object> getCustomFields() {
        return new HashMap<>(this.customFields);
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public Boolean getIsFraud() { return isFraud; }
    public void setIsFraud(Boolean isFraud) { this.isFraud = isFraud; }
    
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', name='" + name + "', age=" + age + ", isFraud=" + isFraud + "}";
    }
}
