package com.codelens.analysis.engine.model;

/**
 * Hallazgo tal y como lo produce un analizador, antes de persistirse.
 *
 * @param ruleId      regla o analizador que lo genero
 * @param title       titulo corto
 * @param description explicacion del riesgo
 * @param category    familia del hallazgo
 * @param severity    gravedad
 * @param confidence  confianza 0..1
 * @param cwe         CWE asociado, puede ser null
 * @param owasp       categoria OWASP, puede ser null
 * @param filePath    archivo afectado; null para hallazgos de proyecto
 * @param lineNumber  linea 1-based; 0 cuando no aplica
 * @param evidence    fragmento de codigo que justifica el hallazgo
 * @param language    lenguaje del archivo
 * @param remediation como corregirlo
 */
public record RawFinding(
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

    public static Builder builder(String ruleId) {
        return new Builder(ruleId);
    }

    /** Constructor fluido: los hallazgos tienen muchos campos opcionales. */
    public static final class Builder {
        private final String ruleId;
        private String title = "";
        private String description = "";
        private FindingCategory category = FindingCategory.SECURITY;
        private Severity severity = Severity.MEDIUM;
        private double confidence = 0.7;
        private String cwe;
        private String owasp;
        private String filePath;
        private int lineNumber;
        private String evidence = "";
        private String language;
        private String remediation;

        private Builder(String ruleId) {
            this.ruleId = ruleId;
        }

        public Builder title(String v) { this.title = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder category(FindingCategory v) { this.category = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder confidence(double v) { this.confidence = v; return this; }
        public Builder cwe(String v) { this.cwe = v; return this; }
        public Builder owasp(String v) { this.owasp = v; return this; }
        public Builder filePath(String v) { this.filePath = v; return this; }
        public Builder lineNumber(int v) { this.lineNumber = v; return this; }
        public Builder evidence(String v) { this.evidence = v; return this; }
        public Builder language(String v) { this.language = v; return this; }
        public Builder remediation(String v) { this.remediation = v; return this; }

        public RawFinding build() {
            return new RawFinding(ruleId, title, description, category, severity, confidence,
                    cwe, owasp, filePath, lineNumber, evidence, language, remediation);
        }
    }
}
