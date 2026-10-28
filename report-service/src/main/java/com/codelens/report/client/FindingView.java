package com.codelens.report.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Hallazgo recibido del analysis-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FindingView(
        Long id,
        String ruleId,
        String title,
        String description,
        String category,
        String severity,
        double confidence,
        String cwe,
        String owasp,
        String filePath,
        int lineNumber,
        String evidence,
        String language,
        String remediation
) {

    /** Peso relativo del hallazgo para ordenar el plan de remediacion. */
    public int severityRank() {
        return switch (severity == null ? "" : severity.toUpperCase()) {
            case "CRITICAL" -> 10;
            case "HIGH" -> 8;
            case "MEDIUM" -> 5;
            case "LOW" -> 3;
            default -> 1;
        };
    }

    public String location() {
        if (filePath == null) {
            return "proyecto";
        }
        return lineNumber > 0 ? filePath + ":" + lineNumber : filePath;
    }
}
