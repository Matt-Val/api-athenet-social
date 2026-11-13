package com.athenet.social.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/*
    Cubre el requisito T05 de la rubrica: el rol viaja en los claims del token.
    Cognito publica los grupos en "cognito:groups" (no en "roles" como Entra ID),
    y Spring exige el prefijo ROLE_ para que hasRole() los encuentre.
*/
class CognitoJwtConfigTest {

    private final JwtAuthenticationConverter converter =
            new CognitoJwtConfig().jwtAuthenticationConverter();

    private static Jwt.Builder accessToken() {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .claim("sub", "9478a468-1001-708c-fc95-c3e9f50b4195")
                .claim("token_use", "access")
                .claim("client_id", "3gbm6r4i5822jglhl3pq34uash")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
    }

    private List<String> authoritiesOf(Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    @DisplayName("el grupo CLIENTE de Cognito se traduce a ROLE_CLIENTE")
    void cognitoGroupBecomesSpringRole() {
        Jwt jwt = accessToken().claim("cognito:groups", List.of("CLIENTE")).build();

        assertThat(authoritiesOf(jwt)).contains("ROLE_CLIENTE");
    }

    @Test
    @DisplayName("el grupo se pasa a mayusculas, para que hasRole lo encuentre igual")
    void groupIsUppercased() {
        Jwt jwt = accessToken().claim("cognito:groups", List.of("cliente")).build();

        assertThat(authoritiesOf(jwt)).contains("ROLE_CLIENTE");
    }

    @Test
    @DisplayName("varios grupos producen varias autoridades")
    void severalGroupsBecomeSeveralRoles() {
        Jwt jwt = accessToken().claim("cognito:groups", List.of("CLIENTE", "MODERADOR")).build();

        assertThat(authoritiesOf(jwt)).contains("ROLE_CLIENTE", "ROLE_MODERADOR");
    }

    @Test
    @DisplayName("un token sin grupos no obtiene ningun rol")
    void tokenWithoutGroupsHasNoRole() {
        Jwt jwt = accessToken().build();

        assertThat(authoritiesOf(jwt)).noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    @DisplayName("los scopes siguen convirtiendose, ademas de los grupos")
    void scopesAreKeptAlongsideGroups() {
        Jwt jwt = accessToken()
                .claim("scope", "openid profile email")
                .claim("cognito:groups", List.of("CLIENTE"))
                .build();

        assertThat(authoritiesOf(jwt)).contains("ROLE_CLIENTE", "SCOPE_openid", "SCOPE_email");
    }

    @Test
    @DisplayName("la identidad del usuario es el sub, nunca el username")
    void principalIsTheSubject() {
        Jwt jwt = accessToken()
                .claim("username", "un-username-cualquiera")
                .claim("cognito:groups", List.of("CLIENTE"))
                .build();

        assertThat(converter.convert(jwt).getName())
                .isEqualTo("9478a468-1001-708c-fc95-c3e9f50b4195");
    }
}
