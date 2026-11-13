package com.athenet.social.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/*
    Solo el id del equipo. Los nombres NO se aceptan desde el cliente: si lo
    hicieramos, cualquiera podria guardar un equipo llamado como quisiera.
    Las columnas team_name e institution_name quedan nulas por ahora; se llenaran
    cuando se agregue el cliente de MS-Instituciones. Mientras tanto el frontend ya
    tiene el catalogo de equipos cargado en sesion y puede resolver el nombre solo.
*/
public record FavoriteTeamRequest(
        @NotNull @Positive Long teamId
) {
}
