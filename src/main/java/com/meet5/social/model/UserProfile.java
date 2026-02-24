package com.meet5.social.model;

public class UserProfile {
    private Long id;
    private String username;
    private String name;
    private Integer age;
    private String profileDescription;
    private String relationshipStatus;
    private String profilePicture1;
    private String profilePicture2;
    private String profilePicture3;
    private String profilePicture4;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getProfileDescription() { return profileDescription; }
    public void setProfileDescription(String profileDescription) { this.profileDescription = profileDescription; }

    public String getRelationshipStatus() { return relationshipStatus; }
    public void setRelationshipStatus(String relationshipStatus) { this.relationshipStatus = relationshipStatus; }

    public String getProfilePicture1() { return profilePicture1; }
    public void setProfilePicture1(String profilePicture1) { this.profilePicture1 = profilePicture1; }

    public String getProfilePicture2() { return profilePicture2; }
    public void setProfilePicture2(String profilePicture2) { this.profilePicture2 = profilePicture2; }

    public String getProfilePicture3() { return profilePicture3; }
    public void setProfilePicture3(String profilePicture3) { this.profilePicture3 = profilePicture3; }

    public String getProfilePicture4() { return profilePicture4; }
    public void setProfilePicture4(String profilePicture4) { this.profilePicture4 = profilePicture4; }
}
