package com.codelens.report.dto;

import com.codelens.report.model.Report;

import java.time.Instant;
import java.util.List;

/** Reporte completo, sin el Markdown para no inflar la respuesta de la lista. */
public record ReportDto(
        String id,
        String analysisId,
        String sourceId,
        String projectName,
        Instant generatedAt,
        String verdict,
        String riskLevel,
        String grade,
        double overallScore,
        double securityScore,
        double healthScore,
        String executiveSummary,
        List<String> strengths,
        List<String> keyRisks,
        List<RecommendationDto> recommendations
) {
    public static ReportDto from(Report r) {
        return new ReportDto(
                r.getId(), r.getAnalysisId(), r.getSourceId(), r.getProjectName(), r.getGeneratedAt(),
                r.getVerdict(), r.getRiskLevel(), r.getGrade(), r.getOverallScore(),
                r.getSecurityScore(), r.getHealthScore(), r.getExecutiveSummary(),
                r.getStrengths(), r.getKeyRisks(),
                r.getRecommendations().stream().map(RecommendationDto::from).toList());
    }
}
