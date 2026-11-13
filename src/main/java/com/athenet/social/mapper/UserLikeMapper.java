package com.athenet.social.mapper;

import org.springframework.stereotype.Component;

import com.athenet.social.dto.response.LikeResponse;
import com.athenet.social.model.UserLike;

@Component
public class UserLikeMapper {

    public LikeResponse toResponse(UserLike like) {
        return new LikeResponse(
                like.getId(),
                like.getTargetType(),
                like.getTargetId(),
                like.getCreatedAt());
    }
}
