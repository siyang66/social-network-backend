package com.meet5.social.mapper;

import com.meet5.social.model.Event;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EventMapper {

    void batchInsert(List<Event> events);
}
