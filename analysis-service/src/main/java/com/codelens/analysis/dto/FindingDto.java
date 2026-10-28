package com.codelens.analysis.dto;

import com.codelens.analysis.engine.model.FindingCategory;
import com.codelens.analysis.engine.model.Severity;
import com.codelens.analysis.model.Finding;

/** Hallazgo tal y como lo consumen el frontend y el servicio de reportes. */
public record FindingDto(
        Long id,
        String ruleId,
        String title,
        String description,
        FindingCategory category,
        Severity severity,
        double confidence,
        String cwe,
        String owasp,
        String filePath,
        int lineNumber,
        String evidence,
        String language,
        String remediation
) {
    public static FindingDto from(Finding f) {
        return new FindingDto(f.getId(), f.getRuleId(), f.getTitle(), f.getDescription(), f.getCategory(),
                f.getSeverity(), f.getConfidence(), f.getCwe(), f.getOwasp(), f.getFilePath(),
                f.getLineNumber(), f.getEvidence(), f.getLanguage(), f.getRemediation());
    }
}
