package com.meet5.social.service;

import com.meet5.social.model.UserLike;

/**
 * Handles like/unlike toggling between users.
 */
public interface LikeService {

    // returns the new like on like, null on unlike
    UserLike toggleLike(Long likerId, Long likedId);
}
