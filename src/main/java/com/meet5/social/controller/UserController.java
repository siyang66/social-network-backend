package com.meet5.social.controller;

import com.meet5.social.common.Result;
import com.meet5.social.model.UserLike;
import com.meet5.social.model.VisitResult;
import com.meet5.social.service.LikeService;
import com.meet5.social.service.VisitService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final VisitService visitService;
    private final LikeService likeService;

    public UserController(VisitService visitService, LikeService likeService) {
        this.visitService = visitService;
        this.likeService = likeService;
    }

    // visitorId comes from the JWT token, not the request params
    @PostMapping("/visit")
    @CircuitBreaker(name = "visitApi", fallbackMethod = "visitFallback")
    @RateLimiter(name = "visitApi", fallbackMethod = "visitFallback")
    @CacheEvict(value = "visitors", key = "#visitedId")
    public ResponseEntity<Result<VisitResult>> recordVisit(
            @AuthenticationPrincipal Long visitorId,
            @RequestParam Long visitedId) {
        if (visitorId.equals(visitedId)) {
            return ResponseEntity.badRequest().body(Result.badRequest("User cannot visit their own profile"));
        }
        VisitResult result = visitService.recordVisit(visitorId, visitedId);
        return ResponseEntity.ok(Result.ok(result));
    }

    // like and unlike are handled by the same endpoint — if the record exists, it's removed
    @PostMapping("/like")
    @CircuitBreaker(name = "likeApi", fallbackMethod = "likeFallback")
    @RateLimiter(name = "likeApi", fallbackMethod = "likeFallback")
    @CacheEvict(value = "likes", key = "#likerId")
    public ResponseEntity<Result<UserLike>> recordLike(
            @AuthenticationPrincipal Long likerId,
            @RequestParam Long likedId) {
        if (likerId.equals(likedId)) {
            return ResponseEntity.badRequest().body(Result.badRequest("User cannot like their own profile"));
        }
        UserLike like = likeService.toggleLike(likerId, likedId);
        if (like == null) {
            return ResponseEntity.ok(Result.ok());
        }
        return ResponseEntity.ok(Result.ok(like));
    }

    // fallback for both circuit breaker and rate limiter
    public ResponseEntity<Result<VisitResult>> visitFallback(Long visitorId, Long visitedId, Throwable t) {
        log.warn("visitApi fallback triggered for visitorId={} visitedId={}: {}", visitorId, visitedId, t.getMessage());
        if (t instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            return ResponseEntity.status(429).body(Result.tooManyRequests());
        }
        return ResponseEntity.status(503).body(Result.serviceUnavailable());
    }

    public ResponseEntity<Result<UserLike>> likeFallback(Long likerId, Long likedId, Throwable t) {
        log.warn("likeApi fallback triggered for likerId={} likedId={}: {}", likerId, likedId, t.getMessage());
        if (t instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            return ResponseEntity.status(429).body(Result.tooManyRequests());
        }
        return ResponseEntity.status(503).body(Result.serviceUnavailable());
    }
}
