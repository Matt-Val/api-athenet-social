package com.athenet.social.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter
@NoArgsConstructor 
@EqualsAndHashCode (of = "id")
@Entity
@Table(name = "user_favorite_teams")
public class UserFavoriteTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_name", length = 256)
    private String teamName;

    @Column(name = "institution_id")
    private Long institutionId;

    @Column(name = "institution_name", length = 256)
    private String institutionName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
