package com.athenet.social;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/*
    Arranque completo del contexto. Es el test mas valioso del servicio aunque no
    afirme nada: Flyway aplica las migraciones y despues Hibernate valida que cada
    entidad calce con su tabla, asi que un nombre de columna mal escrito hace
    fallar la build en vez de reventar en produccion.

    Necesita PostgreSQL arriba (docker compose up -d).

    El JwtDecoder va simulado para que el arranque no dependa de alcanzar Cognito
    por red: el decoder real se construye de forma anticipada y descargaria el
    documento de descubrimiento. Su comportamiento se prueba aparte, en
    CognitoJwtConfigTest y SecurityConfigTest.
*/
@SpringBootTest
@TestPropertySource(properties = {
        "COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_test",
        "COGNITO_CLIENT_ID=client-de-prueba",
        "COGNITO_USERINFO_URI=https://example.invalid/oauth2/userInfo" })
class SocialApplicationTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
