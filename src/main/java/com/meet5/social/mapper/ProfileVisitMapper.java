package com.meet5.social.mapper;

import com.meet5.social.model.ProfileVisit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ProfileVisitMapper {

    void insert(ProfileVisit visit);

    long countVisitsInTimeWindow(
            @Param("visitorId") Long visitorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Long> findRecentVisitorIds(@Param("since") LocalDateTime since);
}
