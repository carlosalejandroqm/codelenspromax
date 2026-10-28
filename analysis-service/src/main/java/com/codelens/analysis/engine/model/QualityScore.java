package com.codelens.analysis.engine.model;

import java.util.Map;

/**
 * Puntuaciones finales del analisis.
 *
 * @param securityScore  0-100, cuanto mas alto menos riesgo de seguridad
 * @param healthScore    0-100, mantenibilidad del codigo
 * @param overallScore   combinacion ponderada de ambos
 * @param grade          calificacion A-F derivada del score global
 * @param riskLevel      nivel de riesgo segun el peor hallazgo confirmado
 * @param subScores      desglose de los factores de salud, para explicar la nota
 */
public record QualityScore(
        double securityScore,
        double healthScore,
        double overallScore,
        String grade,
        Severity riskLevel,
        Map<String, Double> subScores
) {
}
