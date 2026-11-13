package com.athenet.social.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.athenet.social.client.CognitoUserInfoClient;
import com.athenet.social.client.CognitoUserInfoClient.CognitoUserInfo;
import com.athenet.social.model.UserProfile;
import com.athenet.social.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
    Unico lugar del servicio que lee claims de identidad. Traduce el token de
    Cognito al perfil local y lo crea la primera vez que aparece un usuario.

    No lleva @Transactional a proposito: si el metodo completo fuera transaccional,
    la violacion de constraint del rescate de abajo marcaria la transaccion como
    rollback-only y el findBy posterior fallaria tambien.
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserProvider {

    // Cada cuanto se refresca last_seen_at, para no escribir en cada peticion.
    private static final Duration LAST_SEEN_REFRESH = Duration.ofMinutes(5);

    private final UserProfileRepository profiles;
    private final CognitoUserInfoClient userInfoClient;

    public UserProfile resolve(Jwt jwt) {
        String cognitoSub = jwt.getSubject();

        var known = profiles.findByCognitoSub(cognitoSub);
        if (known.isPresent()) {
            return touch(known.get());
        }

        // Sub desconocido: el access token no trae correo ni nombre, se le pregunta a Cognito.
        CognitoUserInfo info = userInfoClient.fetch(jwt.getTokenValue());

        /*
            Re-vinculacion: si el User Pool se recreo, el mismo usuario vuelve con un
            sub nuevo pero el mismo correo. Se actualiza el sub y conserva sus likes y
            equipos favoritos en vez de dejarlos huerfanos.
            No se toca display_name: el usuario pudo haberlo editado.
        */
        var sameEmail = profiles.findByEmailIgnoreCase(info.email());
        if (sameEmail.isPresent()) {
            UserProfile profile = sameEmail.get();
            log.warn("Re-vinculando el perfil {} a un sub nuevo de Cognito", profile.getId());
            profile.setCognitoSub(cognitoSub);
            profile.setGivenName(info.givenName());
            profile.setFamilyName(info.familyName());
            profile.setUpdatedAt(Instant.now());
            profile.setLastSeenAt(Instant.now());
            return profiles.save(profile);
        }

        return create(cognitoSub, info);
    }

    private UserProfile touch(UserProfile profile) {
        Instant now = Instant.now();
        if (profile.getLastSeenAt() != null
                && profile.getLastSeenAt().isAfter(now.minus(LAST_SEEN_REFRESH))) {
            return profile;
        }
        profile.setLastSeenAt(now);
        return profiles.save(profile);
    }

    private UserProfile create(String cognitoSub, CognitoUserInfo info) {
        Instant now = Instant.now();

        UserProfile profile = new UserProfile();
        profile.setCognitoSub(cognitoSub);
        profile.setEmail(info.email());
        profile.setGivenName(info.givenName());
        profile.setFamilyName(info.familyName());
        profile.setDisplayName(defaultDisplayName(info));
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setLastSeenAt(now);

        try {
            return profiles.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Carrera al crear el perfil de {}, releyendo", cognitoSub);
            return profiles.findByCognitoSub(cognitoSub)
                    .or(() -> profiles.findByEmailIgnoreCase(info.email()))
                    .orElseThrow(() -> ex);
        }
    }

    private String defaultDisplayName(CognitoUserInfo info) {
        if (info.givenName() != null && !info.givenName().isBlank()) {
            return info.givenName();
        }
        int at = info.email().indexOf('@');
        return at > 0 ? info.email().substring(0, at) : info.email();
    }
}
