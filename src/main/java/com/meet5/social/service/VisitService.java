package com.meet5.social.service;

import com.meet5.social.model.VisitResult;
import com.meet5.social.model.VisitorInfo;

import java.util.List;

/**
 * Handles profile visit recording and returns the visited user's profile.
 */
public interface VisitService {

    VisitResult recordVisit(Long visitorId, Long visitedId);

    List<VisitorInfo> getVisitors(Long userId);
}
