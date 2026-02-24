package com.meet5.social.service.impl;

import com.meet5.social.common.FraudUserException;
import com.meet5.social.mapper.UserLikeMapper;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.UserLike;
import com.meet5.social.service.FraudDetectionService;
import com.meet5.social.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
// TODO(microservices): extract into a dedicated Interaction Service; replace local calls with Feign/gRPC
public class LikeServiceImpl implements LikeService {

    private final UserLikeMapper userLikeMapper;
    private final FraudDetectionService fraudDetectionService;
    private final UserMapper userMapper;

    public LikeServiceImpl(UserLikeMapper userLikeMapper, FraudDetectionService fraudDetectionService, UserMapper userMapper) {
        this.userLikeMapper = userLikeMapper;
        this.fraudDetectionService = fraudDetectionService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserLike toggleLike(Long likerId, Long likedId) {
        if (userMapper.isFraud(likerId)) throw new FraudUserException(likerId);
        if (userLikeMapper.exists(likerId, likedId)) {
            userLikeMapper.delete(likerId, likedId);
            return null;
        }
        UserLike like = new UserLike(likerId, likedId);
        userLikeMapper.insert(like);
        boolean isFraud = fraudDetectionService.processLikeAndCheckFraud(like);
        like.setFraudDetected(isFraud);
        return like;
    }
}
