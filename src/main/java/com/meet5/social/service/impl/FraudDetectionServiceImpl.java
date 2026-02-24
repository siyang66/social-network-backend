package com.meet5.social.service.impl;

import com.meet5.social.mapper.FraudRecordMapper;
import com.meet5.social.mapper.ProfileVisitMapper;
import com.meet5.social.mapper.UserLikeMapper;
import com.meet5.social.model.FraudRecord;
import com.meet5.social.model.ProfileVisit;
import com.meet5.social.model.UserLike;
import com.meet5.social.service.FraudDetectionService;
import com.meet5.social.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// Flags users who visit AND like N+ profiles within a configurable time window.
// Fraud check runs synchronously on each write (visit or like).
// TODO: for higher throughput, publish a USER_ACTIVITY event to Kafka instead and let a
//       dedicated consumer do the threshold check + markUserAsFraud async.
@Slf4j
@Service
// TODO(microservices): extract into a dedicated Fraud Service; replace local calls with Feign/gRPC
public class FraudDetectionServiceImpl implements FraudDetectionService {

    @Value("${fraud.detection.threshold:100}")
    private int fraudThreshold;

    @Value("${fraud.detection.time-window-minutes:10}")
    private int timeWindowMinutes;

    private final ProfileVisitMapper profileVisitMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserService userService;
    private final FraudRecordMapper fraudRecordMapper;

    public FraudDetectionServiceImpl(ProfileVisitMapper profileVisitMapper,
                                     UserLikeMapper userLikeMapper,
                                     UserService userService,
                                     FraudRecordMapper fraudRecordMapper) {
        this.profileVisitMapper = profileVisitMapper;
        this.userLikeMapper = userLikeMapper;
        this.userService = userService;
        this.fraudRecordMapper = fraudRecordMapper;
    }

    @Override
    public boolean processVisitAndCheckFraud(ProfileVisit visit) {
        LocalDateTime end = visit.getVisitedAt();
        LocalDateTime start = end.minusMinutes(timeWindowMinutes);
        return checkAndMarkFraud(visit.getVisitorId(), start, end);
    }

    @Override
    public boolean processLikeAndCheckFraud(UserLike like) {
        LocalDateTime end = like.getLikedAt();
        LocalDateTime start = end.minusMinutes(timeWindowMinutes);
        return checkAndMarkFraud(like.getLikerId(), start, end);
    }

    private boolean checkAndMarkFraud(Long userId, LocalDateTime start, LocalDateTime end) {
        long visitCount = profileVisitMapper.countVisitsInTimeWindow(userId, start, end);
        long likeCount = userLikeMapper.countLikesInTimeWindow(userId, start, end);

        if (visitCount >= fraudThreshold && likeCount >= fraudThreshold) {
            log.info("Fraud detected for userId={}: visitCount={}, likeCount={}", userId, visitCount, likeCount);
            userService.markUserAsFraud(userId);
            fraudRecordMapper.insert(new FraudRecord(userId, visitCount, likeCount, timeWindowMinutes));
            // alternatively: publish a USER_FRAUD_DETECTED event to Kafka here and let a
            // dedicated consumer handle markUserAsFraud + fraudRecordMapper.insert async,
            // which decouples fraud detection from the write path entirely
            return true;
        }
        return false;
    }
}
