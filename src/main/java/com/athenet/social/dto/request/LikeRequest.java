package com.athenet.social.dto.request;

import com.athenet.social.model.enums.LikeTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/*
    No hay campo de identidad: el dueño del like sale del token. targetId es texto
    porque la identidad publica de un evento es su internalId ("EVT-42"), no un numero.
*/
public record LikeRequest(
        @NotNull LikeTargetType targetType,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._:-]{1,64}$",
                 message = "solo admite letras, numeros y . _ : - (maximo 64)")
        String targetId
) {
}
