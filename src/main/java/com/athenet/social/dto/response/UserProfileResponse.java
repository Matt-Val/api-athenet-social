package com.athenet.social.dto.response;

import java.time.Instant;

/*
    No expone cognito_sub: es un identificador interno, el frontend no lo necesita
    y sacarlo del JSON reduce la superficie expuesta.
*/
public record UserProfileResponse(
        Long id,
        String email,
        String givenName,
        String familyName,
        String displayName,
        Instant createdAt
) {
}
