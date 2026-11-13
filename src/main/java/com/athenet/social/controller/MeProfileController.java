package com.athenet.social.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athenet.social.dto.request.ProfilePatchRequest;
import com.athenet.social.dto.response.UserProfileResponse;
import com.athenet.social.mapper.UserProfileMapper;
import com.athenet.social.model.UserProfile;
import com.athenet.social.service.CurrentUserProvider;
import com.athenet.social.service.UserProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
    Perfil del usuario publico autenticado con Cognito. No hay ningun identificador
    de usuario en la ruta ni en el cuerpo: el dueño siempre sale del token validado.
*/
@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class MeProfileController {

    private final CurrentUserProvider currentUser;
    private final UserProfileService profileService;
    private final UserProfileMapper mapper;

    @GetMapping
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UserProfile profile = currentUser.resolve(jwt);
        return ResponseEntity.ok(mapper.toResponse(profile));
    }

    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateDisplayName(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfilePatchRequest request) {
        UserProfile profile = currentUser.resolve(jwt);
        UserProfile updated = profileService.updateDisplayName(profile, request.displayName());
        return ResponseEntity.ok(mapper.toResponse(updated));
    }
}
