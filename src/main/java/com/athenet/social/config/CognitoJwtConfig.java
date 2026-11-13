package com.athenet.social.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class CognitoJwtConfig {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${athenet.cognito.client-id}") String clientId) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        // Firma, expiracion y emisor.
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

        /*
            El access token de Cognito NO trae "aud": la audiencia viaja en "client_id".
            Sin esta comprobacion, un token emitido para otra aplicacion del mismo
            User Pool serviria para entrar aqui.
        */
        OAuth2TokenValidator<Jwt> expectedClient =
                new JwtClaimValidator<String>("client_id", clientId::equals);

        /*
            Barrera contra confusion de audiencia: Cognito emite tres tipos de token
            (id, access, refresh) firmados con la MISMA llave. Sin esto, un id token
            serviria como credencial de API, que es justo lo que no queremos.
        */
        OAuth2TokenValidator<Jwt> expectedTokenUse =
                new JwtClaimValidator<String>("token_use", "access"::equals);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                withIssuer, expectedClient, expectedTokenUse));

        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Collection<GrantedAuthority> fromScopes = scopeConverter.convert(jwt);
            if (fromScopes != null) {
                authorities.addAll(fromScopes);
            }

            // Cognito publica los grupos del usuario en "cognito:groups".
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups != null) {
                for (String group : groups) {
                    authorities.add(new SimpleGrantedAuthority(
                            "ROLE_" + group.toUpperCase(Locale.ROOT)));
                }
            }
            return authorities;
        });

        // La identidad del usuario es el "sub", nunca el "username".
        converter.setPrincipalClaimName(JwtClaimNames.SUB);
        return converter;
    }
}
