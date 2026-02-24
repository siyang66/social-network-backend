package com.meet5.social.service;

import com.meet5.social.model.VisitResult;

/**
 * Handles profile visit recording and returns the visited user's profile.
 */
public interface VisitService {

    VisitResult recordVisit(Long visitorId, Long visitedId);
}
