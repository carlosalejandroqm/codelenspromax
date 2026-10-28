package com.codelens.analysis.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Peticion de analisis sobre un snapshot ya obtenido.
 *
 * @param sourceId id del snapshot en el source-service
 */
public record AnalysisRequest(
        @NotBlank(message = "sourceId es obligatorio")
        String sourceId
) {
}
