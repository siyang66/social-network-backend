package com.meet5.social.controller;

import com.meet5.social.common.Result;
import com.meet5.social.domain.User;
import com.meet5.social.model.Event;
import com.meet5.social.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/bulk-insert-events")
    public ResponseEntity<Result<Void>> bulkInsertEvents(@RequestBody List<Event> events) {
        userService.bulkInsertEvents(events);
        return ResponseEntity.ok(Result.ok());
    }

    @PostMapping("/bulk-insert-users")
    public ResponseEntity<Result<String>> bulkInsertUsers(@RequestBody List<User> users) {
        log.info("Received bulk insert request for {} users", users.size());
        userService.bulkInsertUsers(users);
        return ResponseEntity.ok(Result.ok("Successfully inserted " + users.size() + " users"));
    }
}
