package com.athenet.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
    Solo el nombre para mostrar es editable. No hay campo de identidad: el dueño
    del perfil sale del token, nunca del cuerpo de la peticion.
*/
public record ProfilePatchRequest(
        @NotBlank @Size(max = 256) String displayName
) {
}
