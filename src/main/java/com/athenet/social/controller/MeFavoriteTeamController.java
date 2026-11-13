package com.athenet.social.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athenet.social.dto.request.FavoriteTeamRequest;
import com.athenet.social.dto.response.FavoriteTeamResponse;
import com.athenet.social.mapper.UserFavoriteTeamMapper;
import com.athenet.social.model.UserProfile;
import com.athenet.social.service.CurrentUserProvider;
import com.athenet.social.service.FavoriteTeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Equipos favoritos del usuario autenticado. El dueño sale del token.
@RestController
@RequestMapping("/api/me/favorite-teams")
@RequiredArgsConstructor
public class MeFavoriteTeamController {

    private final CurrentUserProvider currentUser;
    private final FavoriteTeamService favoriteService;
    private final UserFavoriteTeamMapper mapper;

    @GetMapping
    public ResponseEntity<List<FavoriteTeamResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        UserProfile profile = currentUser.resolve(jwt);
        List<FavoriteTeamResponse> body = favoriteService.list(profile).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<FavoriteTeamResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FavoriteTeamRequest request) {

        UserProfile profile = currentUser.resolve(jwt);
        FavoriteTeamService.FavoriteResult result = favoriteService.add(profile, request.teamId());

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result.favorite()));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long teamId) {

        UserProfile profile = currentUser.resolve(jwt);
        favoriteService.remove(profile, teamId);
        return ResponseEntity.noContent().build();
    }
}
