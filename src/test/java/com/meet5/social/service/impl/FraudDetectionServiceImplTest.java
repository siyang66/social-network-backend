package com.meet5.social.service.impl;

import com.meet5.social.mapper.FraudRecordMapper;
import com.meet5.social.mapper.ProfileVisitMapper;
import com.meet5.social.mapper.UserLikeMapper;
import com.meet5.social.model.ProfileVisit;
import com.meet5.social.model.UserLike;
import com.meet5.social.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceImplTest {

    @Mock ProfileVisitMapper profileVisitMapper;
    @Mock UserLikeMapper userLikeMapper;
    @Mock UserService userService;
    @Mock FraudRecordMapper fraudRecordMapper;
    @InjectMocks FraudDetectionServiceImpl fraudDetectionService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(fraudDetectionService, "fraudThreshold", 100);
        ReflectionTestUtils.setField(fraudDetectionService, "timeWindowMinutes", 10);
    }

    @Test
    void processVisitAndCheckFraud_belowThreshold_notFraud() {
        when(profileVisitMapper.countVisitsInTimeWindow(eq(1L), any(), any())).thenReturn(50L);
        when(userLikeMapper.countLikesInTimeWindow(eq(1L), any(), any())).thenReturn(50L);

        boolean result = fraudDetectionService.processVisitAndCheckFraud(new ProfileVisit(1L, 2L));

        assertThat(result).isFalse();
        verifyNoInteractions(userService, fraudRecordMapper);
    }

    @Test
    void processVisitAndCheckFraud_aboveThreshold_markedAsFraud() {
        when(profileVisitMapper.countVisitsInTimeWindow(eq(1L), any(), any())).thenReturn(150L);
        when(userLikeMapper.countLikesInTimeWindow(eq(1L), any(), any())).thenReturn(120L);

        boolean result = fraudDetectionService.processVisitAndCheckFraud(new ProfileVisit(1L, 2L));

        assertThat(result).isTrue();
        verify(userService).markUserAsFraud(1L);
        verify(fraudRecordMapper).insert(any());
    }

    @Test
    void processLikeAndCheckFraud_aboveThreshold_markedAsFraud() {
        when(profileVisitMapper.countVisitsInTimeWindow(eq(1L), any(), any())).thenReturn(100L);
        when(userLikeMapper.countLikesInTimeWindow(eq(1L), any(), any())).thenReturn(100L);

        boolean result = fraudDetectionService.processLikeAndCheckFraud(new UserLike(1L, 2L));

        assertThat(result).isTrue();
        verify(userService).markUserAsFraud(1L);
        verify(fraudRecordMapper).insert(any());
    }

    @Test
    void processLikeAndCheckFraud_onlyVisitsAboveThreshold_notFraud() {
        when(profileVisitMapper.countVisitsInTimeWindow(eq(1L), any(), any())).thenReturn(200L);
        when(userLikeMapper.countLikesInTimeWindow(eq(1L), any(), any())).thenReturn(10L);

        boolean result = fraudDetectionService.processLikeAndCheckFraud(new UserLike(1L, 2L));

        assertThat(result).isFalse();
        verifyNoInteractions(userService, fraudRecordMapper);
    }

    @Test
    void configurable_threshold_respected() {
        ReflectionTestUtils.setField(fraudDetectionService, "fraudThreshold", 50);
        when(profileVisitMapper.countVisitsInTimeWindow(eq(1L), any(), any())).thenReturn(60L);
        when(userLikeMapper.countLikesInTimeWindow(eq(1L), any(), any())).thenReturn(55L);

        boolean result = fraudDetectionService.processVisitAndCheckFraud(new ProfileVisit(1L, 2L));

        assertThat(result).isTrue();
    }
}
