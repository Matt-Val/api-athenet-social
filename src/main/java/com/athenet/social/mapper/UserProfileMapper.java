package com.athenet.social.mapper;

import org.springframework.stereotype.Component;

import com.athenet.social.dto.response.UserProfileResponse;
import com.athenet.social.model.UserProfile;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getEmail(),
                profile.getGivenName(),
                profile.getFamilyName(),
                profile.getDisplayName(),
                profile.getCreatedAt());
    }
}
