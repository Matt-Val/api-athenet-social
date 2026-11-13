package com.athenet.social.dto.response;

import com.athenet.social.model.enums.LikeTargetType;

// Contador publico de un recurso. No revela quien dio el like, solo cuantos.
public record LikeCountResponse(
        LikeTargetType targetType,
        String targetId,
        long count
) {
}
