package com.meet5.social.controller;

import com.meet5.social.config.JwtFilter;
import com.meet5.social.config.SecurityConfig;
import com.meet5.social.model.UserLike;
import com.meet5.social.model.UserProfile;
import com.meet5.social.model.VisitResult;
import com.meet5.social.model.ProfileVisit;
import com.meet5.social.service.LikeService;
import com.meet5.social.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean VisitService visitService;
    @MockBean LikeService likeService;
    // JwtUtil is needed by JwtFilter
    @MockBean com.meet5.social.config.JwtUtil jwtUtil;

    // helper — sets authenticated userId in SecurityContext for the test thread
    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    // POST /visit — normal case
    @Test
    void recordVisit_success() throws Exception {
        authenticateAs(1L);
        ProfileVisit visit = new ProfileVisit(1L, 2L);
        visit.setId(10L);
        UserProfile profile = new UserProfile();
        profile.setId(2L);
        profile.setUsername("bob");
        when(visitService.recordVisit(1L, 2L)).thenReturn(new VisitResult(visit, profile));

        mockMvc.perform(post("/api/v1/user/visit")
                        .param("visitedId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.visit.id").value(10))
                .andExpect(jsonPath("$.data.profile.username").value("bob"));
    }

    // POST /visit — self-visit rejected
    @Test
    void recordVisit_selfVisit_badRequest() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(post("/api/v1/user/visit")
                        .param("visitedId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(visitService);
    }

    // POST /like — new like
    @Test
    void recordLike_success() throws Exception {
        authenticateAs(1L);
        UserLike like = new UserLike(1L, 2L);
        like.setId(5L);
        when(likeService.toggleLike(1L, 2L)).thenReturn(like);

        mockMvc.perform(post("/api/v1/user/like")
                        .param("likedId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    // POST /like — unlike (service returns null)
    @Test
    void recordLike_unlike_success() throws Exception {
        authenticateAs(1L);
        when(likeService.toggleLike(1L, 2L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/user/like")
                        .param("likedId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // POST /like — self-like rejected
    @Test
    void recordLike_selfLike_badRequest() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(post("/api/v1/user/like")
                        .param("likedId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verifyNoInteractions(likeService);
    }
}
