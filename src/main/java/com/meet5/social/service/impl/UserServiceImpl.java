package com.meet5.social.service.impl;

import com.meet5.social.domain.User;
import com.meet5.social.mapper.UserMapper;
import com.meet5.social.model.Event;
import com.meet5.social.model.UserProfile;
import com.meet5.social.repository.BulkInsertRepository;
import com.meet5.social.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Slf4j
@Service
// TODO(microservices): extract into a dedicated User Service; replace local calls with Feign/gRPC
public class UserServiceImpl implements UserService {

    private final JdbcTemplate jdbcTemplate;
    private final BulkInsertRepository bulkInsertRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(JdbcTemplate jdbcTemplate, BulkInsertRepository bulkInsertRepository, UserMapper userMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.bulkInsertRepository = bulkInsertRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        user.validate();
        log.info("Creating user: username={}, email={}", user.getUsername(), user.getEmail());
        String sql = """
                INSERT INTO users (username, email, name, age, is_fraud, created_at, updated_at)
                VALUES (?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getName());
            ps.setInt(4, user.getAge());
            return ps;
        }, keyHolder);
        user.setId(keyHolder.getKeyAs(Long.class));
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(Long userId) {
        return userMapper.findProfileById(userId);
    }

    @Override
    @Transactional
    public void bulkInsertEvents(List<Event> events) {
        bulkInsertRepository.bulkInsertEvents(events);
    }

    @Override
    @Transactional
    public void markUserAsFraud(Long userId) {
        log.info("Marking userId={} as fraud", userId);
        jdbcTemplate.update(
                "UPDATE users SET is_fraud = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                userId);
    }
}
