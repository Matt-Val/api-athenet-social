package com.athenet.social.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.athenet.social.model.UserProfile;


public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByCognitoSub(String cognitoSub);
    /*
        Respaldo para re-vincular un perfil cuando Cognito emite un sub nuevo
        EJ: si se recrea el User Pool, el email no cambia.
    */
    Optional<UserProfile> findByEmailIgnoreCase(String email);
}
