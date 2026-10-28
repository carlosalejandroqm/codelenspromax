package com.codelens.analysis.dto;

import com.codelens.analysis.engine.model.Severity;
import com.codelens.analysis.model.Analysis;
import com.codelens.analysis.model.AnalysisStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resumen de un analisis. Es el contrato que consume el report-service, por lo
 * que incluye todo lo necesario para redactar el reporte sin volver a pedir
 * datos: scores, conteos por gravedad y por categoria y metricas de salud.
 */
public record AnalysisDto(
        String id,
        String sourceId,
        String projectName,
        AnalysisStatus status,
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
        Severity riskLevel,
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
        List<LanguageStatDto> languages,
        String errorMessage
) {

    public static AnalysisDto from(Analysis a) {
        return new AnalysisDto(
                a.getId(), a.getSourceId(), a.getProjectName(), a.getStatus(), a.getStartedAt(),
                a.getCompletedAt(), a.getDurationMillis(), a.getFilesAnalyzed(), a.getTotalLines(),
                a.getCodeLines(), a.getCommentLines(), a.getBlankLines(), a.getSecurityScore(),
                a.getHealthScore(), a.getOverallScore(), a.getGrade(), a.getRiskLevel(),
                a.getCriticalCount(), a.getHighCount(), a.getMediumCount(), a.getLowCount(),
                a.getInfoCount(), a.getSecurityFindings(), a.getSecretFindings(),
                a.getDependencyFindings(), a.getMaintainabilityFindings(), a.getTechnicalDebtFindings(),
                a.getDuplicationRatio(), a.getAvgComplexity(), a.getMaxComplexity(),
                a.getTestFileRatio(), a.getTodoCount(), a.getDeclaredDependencies(),
                a.getSubScores(), a.getLanguages().stream().map(LanguageStatDto::from).toList(),
                a.getErrorMessage());
    }
}
