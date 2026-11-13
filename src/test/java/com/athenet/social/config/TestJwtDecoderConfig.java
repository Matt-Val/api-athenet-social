package com.athenet.social.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/*
    El decoder va simulado para que los tests no salgan a la red a buscar las claves
    de Cognito. El converter, en cambio, es el REAL: queremos que el test ejercite
    el mapeo de cognito:groups a ROLE_*, no una version de mentira.
*/
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new CognitoJwtConfig().jwtAuthenticationConverter();
    }
}
