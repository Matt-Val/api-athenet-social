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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.athenet.social.dto.request.LikeRequest;
import com.athenet.social.dto.response.LikeResponse;
import com.athenet.social.mapper.UserLikeMapper;
import com.athenet.social.model.UserProfile;
import com.athenet.social.model.enums.LikeTargetType;
import com.athenet.social.service.CurrentUserProvider;
import com.athenet.social.service.LikeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// "Me gusta" del usuario autenticado. El dueño sale del token, nunca de la ruta.
@RestController
@RequestMapping("/api/me/likes")
@RequiredArgsConstructor
public class MeLikeController {

    private final CurrentUserProvider currentUser;
    private final LikeService likeService;
    private final UserLikeMapper mapper;

    @GetMapping
    public ResponseEntity<List<LikeResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LikeTargetType targetType) {

        UserProfile profile = currentUser.resolve(jwt);
        List<LikeResponse> body = likeService.list(profile, targetType).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<LikeResponse> like(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LikeRequest request) {

        UserProfile profile = currentUser.resolve(jwt);
        LikeService.LikeResult result =
                likeService.like(profile, request.targetType(), request.targetId());

        // 201 si se creo, 200 si ya existia: dar like dos veces no es un error.
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(mapper.toResponse(result.like()));
    }

    @DeleteMapping("/{targetType}/{targetId}")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable LikeTargetType targetType,
            @PathVariable String targetId) {

        UserProfile profile = currentUser.resolve(jwt);
        likeService.unlike(profile, targetType, targetId);
        return ResponseEntity.noContent().build();
    }
}
