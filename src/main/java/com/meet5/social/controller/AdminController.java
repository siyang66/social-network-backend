package com.meet5.social.controller;

import com.meet5.social.common.Result;
import com.meet5.social.model.Event;
import com.meet5.social.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
