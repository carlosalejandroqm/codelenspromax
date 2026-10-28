package com.codelens.report.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Proyeccion del analisis que publica el analysis-service. Copia local del
 * contrato: si el otro servicio anade campos, este no se rompe gracias a
 * {@code ignoreUnknown}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisView(
        String id,
        String sourceId,
        String projectName,
        String status,
        Instant startedAt,
        Instant completedAt,
        long durationMillis,
        int filesAnalyzed,
        long totalLines,
        long codeLines,
        long commentLines,
        long blankLines,
        double securityScore,
        double healthScore,
        double overallScore,
        String grade,
        String riskLevel,
        int criticalCount,
        int highCount,
        int mediumCount,
        int lowCount,
        int infoCount,
        int securityFindings,
        int secretFindings,
        int dependencyFindings,
        int maintainabilityFindings,
        int technicalDebtFindings,
        double duplicationRatio,
        double avgComplexity,
        int maxComplexity,
        double testFileRatio,
        int todoCount,
        int declaredDependencies,
        Map<String, Double> subScores,
        List<LanguageStatView> languages
) {

    /** Reparto de lenguajes del proyecto analizado. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LanguageStatView(String language, int files, long codeLines, double percentage) {
    }

    public int totalFindings() {
        return criticalCount + highCount + mediumCount + lowCount + infoCount;
    }

    public int blockingFindings() {
        return criticalCount + highCount;
    }
}
