package com.athenet.social.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.athenet.social.model.UserProfile;
import com.athenet.social.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profiles;

    /*
        Recibe el perfil ya resuelto por CurrentUserProvider, no un id: asi no existe
        ninguna firma en la que se pueda pasar el perfil de otra persona.
    */
    public UserProfile updateDisplayName(UserProfile profile, String displayName) {
        profile.setDisplayName(displayName.trim());
        profile.setUpdatedAt(Instant.now());
        return profiles.save(profile);
    }
}
