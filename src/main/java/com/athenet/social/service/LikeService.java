package com.athenet.social.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.athenet.social.model.UserLike;
import com.athenet.social.model.UserProfile;
import com.athenet.social.model.enums.LikeTargetType;
import com.athenet.social.repository.LikeCountProjection;
import com.athenet.social.repository.UserLikeRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class LikeService {

    // Tope del conteo por lote, para que nadie pida diez mil ids en una URL.
    private static final int MAX_BATCH = 100;

    private final UserLikeRepository likes;

    // created = false cuando el like ya existia; el controlador lo traduce a 200 en vez de 201.
    public record LikeResult(UserLike like, boolean created) {
    }

    public List<UserLike> list(UserProfile profile, LikeTargetType targetType) {
        return targetType == null
                ? likes.findByUserProfileIdOrderByCreatedAtDesc(profile.getId())
                : likes.findByUserProfileIdAndTargetTypeOrderByCreatedAtDesc(profile.getId(), targetType);
    }

    public LikeResult like(UserProfile profile, LikeTargetType targetType, String targetId) {
        var existing = likes.findByUserProfileIdAndTargetTypeAndTargetId(
                profile.getId(), targetType, targetId);
        if (existing.isPresent()) {
            return new LikeResult(existing.get(), false);
        }

        UserLike like = new UserLike();
        like.setUserProfileId(profile.getId());
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        like.setCreatedAt(Instant.now());

        try {
            return new LikeResult(likes.saveAndFlush(like), true);
        } catch (DataIntegrityViolationException ex) {
            return likes.findByUserProfileIdAndTargetTypeAndTargetId(
                            profile.getId(), targetType, targetId)
                    .map(found -> new LikeResult(found, false))
                    .orElseThrow(() -> ex);
        }
    }

    // Borrar algo que ya no esta no es un error: el resultado final es el mismo.
    public void unlike(UserProfile profile, LikeTargetType targetType, String targetId) {
        likes.findByUserProfileIdAndTargetTypeAndTargetId(profile.getId(), targetType, targetId)
                .ifPresent(likes::delete);
    }

    public long countFor(LikeTargetType targetType, String targetId) {
        return likes.countByTargetTypeAndTargetId(targetType, targetId);
    }

    /*
        Devuelve el conteo de cada id pedido, conservando el orden y rellenando con 0
        los que no tienen ningun like (el GROUP BY no los devuelve).
    */
    public Map<String, Long> countsFor(LikeTargetType targetType, List<String> targetIds) {
        List<String> unique = targetIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(MAX_BATCH)
                .toList();

        Map<String, Long> result = new LinkedHashMap<>();
        if (unique.isEmpty()) {
            return result;
        }

        Map<String, Long> found = new LinkedHashMap<>();
        for (LikeCountProjection row : likes.countGroupedByTargetId(targetType, unique)) {
            found.put(row.getTargetId(), row.getTotal());
        }
        for (String id : unique) {
            result.put(id, found.getOrDefault(id, 0L));
        }
        return result;
    }
}
