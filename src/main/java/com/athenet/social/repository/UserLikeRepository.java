package com.athenet.social.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.athenet.social.model.UserLike;
import com.athenet.social.model.enums.LikeTargetType;

public interface UserLikeRepository extends JpaRepository<UserLike, Long> {

    List<UserLike> findByUserProfileIdOrderByCreatedAtDesc(Long userProfileId);

    List<UserLike> findByUserProfileIdAndTargetTypeOrderByCreatedAtDesc(
            Long userProfileId, LikeTargetType targetType);

    Optional<UserLike> findByUserProfileIdAndTargetTypeAndTargetId(
            Long userProfileId, LikeTargetType targetType, String targetId);

    // Para el contador publico de un recurso.
    long countByTargetTypeAndTargetId(LikeTargetType targetType, String targetId);

    /*
        Conteo de varios recursos en una sola consulta: una grilla de tarjetas pide
        todos sus contadores de golpe en vez de una peticion por tarjeta.
        Los recursos sin ningun like NO aparecen en el resultado de un GROUP BY;
        el servicio los completa con 0.
    */
    @Query("""
            SELECT l.targetId AS targetId, COUNT(l) AS total
            FROM UserLike l
            WHERE l.targetType = :targetType AND l.targetId IN :targetIds
            GROUP BY l.targetId
            """)
    List<LikeCountProjection> countGroupedByTargetId(
            @Param("targetType") LikeTargetType targetType,
            @Param("targetIds") Collection<String> targetIds);
}
