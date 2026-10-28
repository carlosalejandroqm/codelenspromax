package com.codelens.analysis.dto;

import java.util.List;

/**
 * Analisis con sus hallazgos, que es lo que pide el frontend al abrir el detalle
 * y el report-service al generar recomendaciones.
 */
public record AnalysisDetailDto(
        AnalysisDto analysis,
        List<FindingDto> findings
) {
}
