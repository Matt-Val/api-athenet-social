package com.athenet.social.dto.response;

import java.time.Instant;

public record FavoriteTeamResponse(
        Long id,
        Long teamId,
        String teamName,
        Long institutionId,
        String institutionName,
        Instant createdAt
) {
}
