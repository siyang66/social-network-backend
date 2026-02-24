package com.meet5.social.service.impl;

import com.meet5.social.common.FraudUserException;
import com.meet5.social.common.UserNotFoundException;
import com.meet5.social.mapper.ProfileVisitMapper;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.ProfileVisit;
import com.meet5.social.model.UserProfile;
import com.meet5.social.model.VisitResult;
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
class VisitServiceImplTest {

    @Mock ProfileVisitMapper profileVisitMapper;
    @Mock FraudDetectionService fraudDetectionService;
    @Mock UserMapper userMapper;
    @InjectMocks VisitServiceImpl visitService;

    @Test
    void recordVisit_fraudUser_throws() {
        when(userMapper.isFraud(1L)).thenReturn(true);

        assertThatThrownBy(() -> visitService.recordVisit(1L, 2L))
                .isInstanceOf(FraudUserException.class);

        verifyNoInteractions(profileVisitMapper, fraudDetectionService);
    }

    @Test
    void recordVisit_returnsVisitAndProfile() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        UserProfile profile = new UserProfile();
        profile.setId(2L);
        when(fraudDetectionService.processVisitAndCheckFraud(any())).thenReturn(false);
        when(userMapper.findProfileById(2L)).thenReturn(profile);

        VisitResult result = visitService.recordVisit(1L, 2L);

        verify(profileVisitMapper).insert(any(ProfileVisit.class));
        assertThat(result.getVisit().getVisitorId()).isEqualTo(1L);
        assertThat(result.getProfile().getId()).isEqualTo(2L);
        assertThat(result.getVisit().isFraudDetected()).isFalse();
    }

    @Test
    void recordVisit_visitedUserNotFound_throws() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        when(fraudDetectionService.processVisitAndCheckFraud(any())).thenReturn(false);
        when(userMapper.findProfileById(2L)).thenReturn(null);

        assertThatThrownBy(() -> visitService.recordVisit(1L, 2L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void recordVisit_fraudDetected() {
        when(userMapper.isFraud(1L)).thenReturn(false);
        when(fraudDetectionService.processVisitAndCheckFraud(any())).thenReturn(true);
        when(userMapper.findProfileById(2L)).thenReturn(new UserProfile());

        VisitResult result = visitService.recordVisit(1L, 2L);

        assertThat(result.getVisit().isFraudDetected()).isTrue();
    }
}
