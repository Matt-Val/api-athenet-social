package com.athenet.social.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.athenet.social.dto.response.LikeCountResponse;
import com.athenet.social.model.enums.LikeTargetType;
import com.athenet.social.service.LikeService;

import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;

/*
    Contadores publicos, sin token. Solo devuelven cuantos likes tiene un recurso,
    nunca quien los dio: un visitante anonimo no puede deducir nada de nadie.
*/
@RestController
@RequestMapping("/api/public/likes")
@RequiredArgsConstructor
public class PublicLikeController {

    private final LikeService likeService;

    @GetMapping("/count")
    public ResponseEntity<LikeCountResponse> count(
            @RequestParam LikeTargetType targetType,
            @RequestParam String targetId) {

        long total = likeService.countFor(targetType, targetId);
        return ResponseEntity.ok(new LikeCountResponse(targetType, targetId, total));
    }

    // Lote: una grilla de tarjetas pide todos sus contadores en una sola peticion.
    @GetMapping("/counts")
    public ResponseEntity<List<LikeCountResponse>> counts(
            @RequestParam LikeTargetType targetType,
            @RequestParam @NotEmpty List<String> targetIds) {

        Map<String, Long> counts = likeService.countsFor(targetType, targetIds);
        List<LikeCountResponse> body = counts.entrySet().stream()
                .map(e -> new LikeCountResponse(targetType, e.getKey(), e.getValue()))
                .toList();
        return ResponseEntity.ok(body);
    }
}
