package com.codelens.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Se puede iniciar sesion con el usuario o con el correo. */
public record LoginRequest(
        @NotBlank(message = "el usuario o correo es obligatorio")
        String username,

        @NotBlank(message = "la contrasena es obligatoria")
        String password
) {
}
