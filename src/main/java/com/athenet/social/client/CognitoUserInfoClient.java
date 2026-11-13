package com.athenet.social.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.athenet.social.exception.UpstreamServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CognitoUserInfoClient {

     private final RestClient restClient;

    public CognitoUserInfoClient(@Value("${athenet.cognito.userinfo-uri}") String userInfoUri) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(userInfoUri)
                .requestFactory(factory)
                .build();
    }

    public CognitoUserInfo fetch(String accessToken) {
        try {
            CognitoUserInfo info = restClient.get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(CognitoUserInfo.class);

            if (info == null || info.email() == null || info.email().isBlank()) {
                throw new UpstreamServiceException("Cognito no devolvio el correo del usuario.");
            }
            return info;

        } catch (UpstreamServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Fallo la consulta a userInfo de Cognito: {}", ex.getMessage());
            throw new UpstreamServiceException("No se pudo obtener el perfil desde Cognito.", ex);
        }
    }

    /*
        ignoreUnknown = true a proposito: Cognito devuelve campos extra (username,
        email_verified y otros) y la configuracion global del servicio es ESTRICTA.
        Esa estrictez protege los cuerpos que recibimos del cliente; una respuesta
        de un tercero tiene que ser tolerante, o el dia que Cognito agregue un campo
        se cae el login. Es la misma regla que el proyecto ya aplica a los
        consumidores de RabbitMQ.
    */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CognitoUserInfo(
            String sub,
            String email,
            @JsonProperty("given_name") String givenName,
            @JsonProperty("family_name") String familyName
    ) {
    
    }
}
