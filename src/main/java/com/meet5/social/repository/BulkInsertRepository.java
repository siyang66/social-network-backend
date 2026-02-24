package com.meet5.social.repository;

import com.meet5.social.mapper.EventMapper;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.Event;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BulkInsertRepository {

    private final UserMapper userMapper;
    private final EventMapper eventMapper;

    public BulkInsertRepository(UserMapper userMapper, EventMapper eventMapper) {
        this.userMapper = userMapper;
        this.eventMapper = eventMapper;
    }

    public void bulkInsertEvents(List<Event> events) {
        if (events == null || events.isEmpty()) return;
        eventMapper.batchInsert(events);
    }
}
