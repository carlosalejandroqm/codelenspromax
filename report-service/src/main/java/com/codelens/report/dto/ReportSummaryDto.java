package com.codelens.report.dto;

import com.codelens.report.model.Report;

import java.time.Instant;

/** Fila de la lista de reportes. */
public record ReportSummaryDto(
        String id,
        String analysisId,
        String projectName,
        Instant generatedAt,
        String verdict,
        String grade,
        double overallScore,
        int recommendations
) {
    public static ReportSummaryDto from(Report r) {
        return new ReportSummaryDto(r.getId(), r.getAnalysisId(), r.getProjectName(), r.getGeneratedAt(),
                r.getVerdict(), r.getGrade(), r.getOverallScore(), r.getRecommendations().size());
    }
}
