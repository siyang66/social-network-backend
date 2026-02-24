package com.meet5.social.model;

public class VisitResult {
    private ProfileVisit visit;
    private UserProfile profile;

    public VisitResult(ProfileVisit visit, UserProfile profile) {
        this.visit = visit;
        this.profile = profile;
    }

    public ProfileVisit getVisit() { return visit; }
    public UserProfile getProfile() { return profile; }
}
