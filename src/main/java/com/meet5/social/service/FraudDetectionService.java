package com.meet5.social.service;

import com.meet5.social.model.ProfileVisit;
import com.meet5.social.model.UserLike;

/**
 * Evaluates fraud rules on visit and like events, marks users, and writes fraud records.
 */
public interface FraudDetectionService {

    boolean processVisitAndCheckFraud(ProfileVisit visit);

    boolean processLikeAndCheckFraud(UserLike like);
}
