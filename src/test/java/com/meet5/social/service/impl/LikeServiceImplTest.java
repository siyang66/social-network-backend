package com.meet5.social.service.impl;

import com.meet5.social.common.FraudUserException;
import com.meet5.social.mapper.UserLikeMapper;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.UserLike;
import com.meet5.social.service.FraudDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock UserLikeMapper userLikeMapper;
    @Mock FraudDetectionService fraudDetectionService;
    @Mock UserMapper userMapper;
    @InjectMocks LikeServiceImpl likeService;

    @Test
    void toggleLike_fraudUser_throws() {
        when(userMapper.isFraud(1L)).thenReturn(true);

        assertThatThrownBy(() -> likeService.toggleLike(1L, 2L))
                .isInstanceOf(FraudUserException.class);

        verifyNoInteractions(userLikeMapper, fraudDetectionService);
    }

    @Test
    void toggleLike_newLike_returnsLikeWithFraudFlag() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        when(userLikeMapper.exists(1L, 2L)).thenReturn(false);
        when(fraudDetectionService.processLikeAndCheckFraud(any())).thenReturn(true);

        UserLike result = likeService.toggleLike(1L, 2L);

        verify(userLikeMapper).insert(any(UserLike.class));
        assertThat(result.isFraudDetected()).isTrue();
    }

    @Test
    void toggleLike_unlike_returnsNull() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        when(userLikeMapper.exists(1L, 2L)).thenReturn(true);

        UserLike result = likeService.toggleLike(1L, 2L);

        verify(userLikeMapper).delete(1L, 2L);
        assertThat(result).isNull();
    }

    @Test
    void toggleLike_noFraud() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        when(userLikeMapper.exists(1L, 2L)).thenReturn(false);
        when(fraudDetectionService.processLikeAndCheckFraud(any())).thenReturn(false);

        UserLike result = likeService.toggleLike(1L, 2L);

        assertThat(result.isFraudDetected()).isFalse();
    }
}
