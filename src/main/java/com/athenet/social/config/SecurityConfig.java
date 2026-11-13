package com.athenet.social.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration 
@EnableWebSecurity
public class SecurityConfig {

    @Value("${athenet.cors.public-origins}")
    private List<String> publicOrigins;

    @Value("${athenet.cors.user-origins}")
    private List<String> userOrigins;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                /*
                    Usuario publico autenticado con Cognito, operando sobre sus propios
                    datos. El dueño del recurso es el sujeto del token y se resuelve en
                    el servicio, nunca en la ruta.
                */
                .requestMatchers("/api/me/**").hasRole("CLIENTE")
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Lecturas anonimas desde el sitio publico: solo GET y sin credenciales.
        CorsConfiguration publicCfg = new CorsConfiguration();
        publicCfg.setAllowedOrigins(publicOrigins);
        publicCfg.setAllowedMethods(List.of("GET", "OPTIONS"));
        publicCfg.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin"));
        publicCfg.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/public/**", publicCfg);

        // Rutas del usuario autenticado: aqui SI viaja la cabecera Authorization.
        CorsConfiguration userCfg = new CorsConfiguration();
        userCfg.setAllowedOrigins(userOrigins);
        userCfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        userCfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        userCfg.setAllowCredentials(false);
        userCfg.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/me/**", userCfg);

        return source;
    }
}
