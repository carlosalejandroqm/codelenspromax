package com.codelens.report.dto;

import com.codelens.report.model.Recommendation;

import java.util.List;

/** Recomendacion tal y como la consume el frontend. */
public record RecommendationDto(
        int rank,
        String priority,
        String priorityLabel,
        String timeWindow,
        String effort,
        String effortEstimate,
        String title,
        String problem,
        String action,
        String rationale,
        String category,
        String cwe,
        String owasp,
        String ownerRole,
        int findingsCount,
        int affectedFiles,
        List<String> examples,
        List<String> ruleIds,
        List<String> references
) {
    public static RecommendationDto from(Recommendation r) {
        return new RecommendationDto(
                r.getRank(), r.getPriority().name(), r.getPriority().getLabel(),
                r.getPriority().getWindow(), r.getEffort().name(), r.getEffort().getEstimate(),
                r.getTitle(), r.getProblem(), r.getAction(), r.getRationale(), r.getCategory(),
                r.getCwe(), r.getOwasp(), r.getOwnerRole(), r.getFindingsCount(), r.getAffectedFiles(),
                r.getExamples(), r.getRuleIds(), r.getReferences());
    }
}
