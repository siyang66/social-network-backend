package com.meet5.social.mapper;

import com.meet5.social.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserProfile findProfileById(@Param("userId") Long userId);

    boolean isFraud(@Param("userId") Long userId);
}
