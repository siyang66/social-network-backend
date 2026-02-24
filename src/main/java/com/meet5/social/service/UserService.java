package com.meet5.social.service;

import com.meet5.social.domain.User;
import com.meet5.social.model.Event;
import com.meet5.social.model.UserProfile;

import java.util.List;

/**
 * Handles user creation, profile retrieval, bulk event inserts, and fraud marking.
 */
public interface UserService {

    User createUser(User user);

    UserProfile getProfile(Long userId);

    void bulkInsertEvents(List<Event> events);

    void markUserAsFraud(Long userId);
}
