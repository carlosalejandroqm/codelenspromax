package com.codelens.analysis.engine.model;

import java.util.List;
import java.util.Map;

/**
 * Resultado completo de una ejecucion del motor.
 *
 * @param findings       hallazgos detectados
 * @param metrics        metricas por archivo
 * @param languages      reparto de lenguajes del proyecto
 * @param score          notas de seguridad y salud
 * @param gauges         metricas numericas auxiliares publicadas por analizadores
 * @param suppressed     hallazgos omitidos por regla al alcanzar el tope
 * @param filesAnalyzed  archivos que entraron al analisis
 * @param durationMillis tiempo de calculo del motor
 */
public record EngineOutcome(
        List<RawFinding> findings,
        List<FileMetrics> metrics,
        List<LanguageUsage> languages,
        QualityScore score,
        Map<String, Double> gauges,
        Map<String, Integer> suppressed,
        int filesAnalyzed,
        long durationMillis
) {
}
