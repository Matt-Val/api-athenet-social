package com.athenet.social.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor 
@EqualsAndHashCode(of = "id")
@Entity 
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Claim "sub" del token ya validado. Nunca llega desde el cuerpo de una petición.
    @Column(name = "cognito_sub", nullable = false, length = 36)
    private String cognitoSub;

    // Si user Pool se recrea, el sub cambia y el email no.
    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "given_name", length = 128)
    private String givenName;

    @Column(name = "family_name", length = 128)
    private String familyName;

    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;
}
