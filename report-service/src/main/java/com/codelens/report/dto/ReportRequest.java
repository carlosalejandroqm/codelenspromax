package com.codelens.report.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Peticion de generacion de reporte.
 *
 * @param analysisId id del analisis completado en el analysis-service
 */
public record ReportRequest(
        @NotBlank(message = "analysisId es obligatorio")
        String analysisId
) {
}
