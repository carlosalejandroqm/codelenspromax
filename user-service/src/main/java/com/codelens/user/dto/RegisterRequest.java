package com.codelens.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "el usuario es obligatorio")
        @Size(min = 3, max = 50, message = "debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "solo letras, numeros, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "no es un correo valido")
        String email,

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "maximo 120 caracteres")
        String fullName,

        @NotBlank(message = "la contrasena es obligatoria")
        @Size(min = 6, max = 72, message = "debe tener entre 6 y 72 caracteres")
        String password
) {
}
