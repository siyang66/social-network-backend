package com.meet5.social.service.impl;

import com.meet5.social.common.FraudUserException;
import com.meet5.social.common.UserNotFoundException;
import com.meet5.social.mapper.ProfileVisitMapper;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.ProfileVisit;
import com.meet5.social.model.UserProfile;
import com.meet5.social.model.VisitResult;
import com.meet5.social.service.FraudDetectionService;
import com.meet5.social.service.VisitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
// TODO(microservices): extract into a dedicated Interaction Service; replace local calls with Feign/gRPC
public class VisitServiceImpl implements VisitService {

    private final ProfileVisitMapper profileVisitMapper;
    private final FraudDetectionService fraudDetectionService;
    private final UserMapper userMapper;

    public VisitServiceImpl(ProfileVisitMapper profileVisitMapper,
                            FraudDetectionService fraudDetectionService,
                            UserMapper userMapper) {
        this.profileVisitMapper = profileVisitMapper;
        this.fraudDetectionService = fraudDetectionService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public VisitResult recordVisit(Long visitorId, Long visitedId) {
        if (userMapper.isFraud(visitorId)) throw new FraudUserException(visitorId);
        ProfileVisit visit = new ProfileVisit(visitorId, visitedId);
        profileVisitMapper.insert(visit);
        boolean isFraud = fraudDetectionService.processVisitAndCheckFraud(visit);
        visit.setFraudDetected(isFraud);
        UserProfile profile = userMapper.findProfileById(visitedId);
        if (profile == null) throw new UserNotFoundException(visitedId);
        return new VisitResult(visit, profile);
    }
}
