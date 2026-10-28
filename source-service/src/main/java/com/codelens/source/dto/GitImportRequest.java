package com.codelens.source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Peticion de importacion de un repositorio remoto.
 *
 * @param repositoryUrl URL https del repositorio (git@ no soportado: requiere claves)
 * @param branch        rama o tag a clonar; null usa la rama por defecto
 * @param accessToken   token para repos privados; nunca se persiste
 */
public record GitImportRequest(
        @NotBlank(message = "repositoryUrl es obligatorio")
        @Pattern(regexp = "^https?://.+", message = "solo se aceptan URLs http/https")
        String repositoryUrl,

        String branch,

        String accessToken
) {
}
