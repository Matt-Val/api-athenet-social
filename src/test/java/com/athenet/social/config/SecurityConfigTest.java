package com.athenet.social.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.athenet.social.controller.MeFavoriteTeamController;
import com.athenet.social.controller.MeLikeController;
import com.athenet.social.controller.MeProfileController;
import com.athenet.social.controller.PublicLikeController;
import com.athenet.social.mapper.UserFavoriteTeamMapper;
import com.athenet.social.mapper.UserLikeMapper;
import com.athenet.social.mapper.UserProfileMapper;
import com.athenet.social.model.UserProfile;
import com.athenet.social.service.CurrentUserProvider;
import com.athenet.social.service.FavoriteTeamService;
import com.athenet.social.service.LikeService;
import com.athenet.social.service.UserProfileService;

/*
    Cubre T05 y F32 de la rubrica: cada ruta exige el rol que corresponde, y un
    usuario publico autenticado sin el grupo CLIENTE queda bloqueado.

    El 403 se prueba aqui y no contra Cognito porque .with(jwt()) fabrica un token
    ya validado con las autoridades que uno quiera: no hace falta crear un segundo
    usuario real en el User Pool solo para comprobar el rechazo.
*/
@WebMvcTest(controllers = {
        MeProfileController.class,
        MeLikeController.class,
        MeFavoriteTeamController.class,
        PublicLikeController.class })
@Import({ SecurityConfig.class, TestJwtDecoderConfig.class,
        UserProfileMapper.class, UserLikeMapper.class, UserFavoriteTeamMapper.class })
@TestPropertySource(properties = {
        // Vacio para que no intente descubrir el emisor real al levantar el contexto.
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "athenet.cognito.client-id=client-de-prueba",
        "athenet.cors.public-origins=http://localhost:5173",
        "athenet.cors.user-origins=http://localhost:5173" })
class SecurityConfigTest {

    private static final SimpleGrantedAuthority CLIENTE = new SimpleGrantedAuthority("ROLE_CLIENTE");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private LikeService likeService;

    @MockitoBean
    private FavoriteTeamService favoriteTeamService;

    @BeforeEach
    void stubCurrentUser() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setCognitoSub("9478a468-1001-708c-fc95-c3e9f50b4195");
        profile.setEmail("matias@example.com");
        profile.setDisplayName("Matias");
        profile.setCreatedAt(Instant.now());
        when(currentUserProvider.resolve(any(Jwt.class))).thenReturn(profile);
    }

    // ── Sin token ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("las rutas /api/me/** sin token devuelven 401")
    void privateRoutesWithoutTokenAreUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/likes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/favorite-teams")).andExpect(status().isUnauthorized());
    }

    // ── Token valido pero SIN el grupo CLIENTE (requisito F32) ───────────────

    @Test
    @DisplayName("un token valido sin el grupo CLIENTE recibe 403, no 401")
    void validTokenWithoutClienteRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/me/profile").with(jwt())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me/likes").with(jwt())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/me/favorite-teams").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tampoco puede escribir sin el grupo")
    void writesWithoutClienteRoleAreForbidden() throws Exception {
        mockMvc.perform(post("/api/me/likes")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"EVENT\",\"targetId\":\"EVT-42\"}"))
                .andExpect(status().isForbidden());
    }

    // ── Token con el grupo CLIENTE ───────────────────────────────────────────

    @Test
    @DisplayName("con ROLE_CLIENTE llega al controlador")
    void clienteRoleReachesTheController() throws Exception {
        mockMvc.perform(get("/api/me/profile").with(jwt().authorities(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("matias@example.com"))
                .andExpect(jsonPath("$.displayName").value("Matias"));

        mockMvc.perform(get("/api/me/likes").with(jwt().authorities(CLIENTE)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/favorite-teams").with(jwt().authorities(CLIENTE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("la respuesta del perfil nunca expone el sub de Cognito")
    void profileResponseHidesTheCognitoSub() throws Exception {
        mockMvc.perform(get("/api/me/profile").with(jwt().authorities(CLIENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cognitoSub").doesNotExist());
    }

    // ── Rutas publicas ───────────────────────────────────────────────────────

    @Test
    @DisplayName("el contador publico responde sin token")
    void publicCountIsAnonymous() throws Exception {
        when(likeService.countFor(any(), any())).thenReturn(7L);

        mockMvc.perform(get("/api/public/likes/count")
                        .param("targetType", "EVENT")
                        .param("targetId", "EVT-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    // ── denyAll por defecto ──────────────────────────────────────────────────

    @Test
    @DisplayName("una ruta no declarada queda rechazada por el denyAll final")
    void undeclaredRouteIsRejected() throws Exception {
        mockMvc.perform(get("/api/loquesea")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/loquesea").with(jwt().authorities(CLIENTE)))
                .andExpect(status().isForbidden());
    }
}
