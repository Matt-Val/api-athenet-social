package com.athenet.social.dto.response;

import java.time.Instant;

import com.athenet.social.model.enums.LikeTargetType;

public record LikeResponse(
        Long id,
        LikeTargetType targetType,
        String targetId,
        Instant createdAt
) {
}
