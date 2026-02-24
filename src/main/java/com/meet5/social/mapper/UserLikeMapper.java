package com.meet5.social.mapper;

import com.meet5.social.model.UserLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserLikeMapper {

    void insert(UserLike like);

    List<Long> findLikedIdsByLikerId(@Param("likerId") Long likerId);

    boolean exists(@Param("likerId") Long likerId, @Param("likedId") Long likedId);

    void delete(@Param("likerId") Long likerId, @Param("likedId") Long likedId);

    long countLikesInTimeWindow(
            @Param("likerId") Long likerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
