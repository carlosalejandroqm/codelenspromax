package com.codelens.analysis.model;

import com.codelens.analysis.engine.model.FindingCategory;
import com.codelens.analysis.engine.model.RawFinding;
import com.codelens.analysis.engine.model.Severity;
import jakarta.persistence.*;

/** Hallazgo persistido de un analisis. */
@Entity
@Table(name = "finding", indexes = {
        @Index(name = "idx_finding_analysis", columnList = "analysis_id"),
        @Index(name = "idx_finding_severity", columnList = "severity")
})
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false, length = 96)
    private String ruleId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FindingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    private double confidence;

    @Column(length = 32)
    private String cwe;

    @Column(length = 96)
    private String owasp;

    @Column(length = 1024)
    private String filePath;

    private int lineNumber;

    @Column(length = 512)
    private String evidence;

    @Column(length = 64)
    private String language;

    @Column(length = 1200)
    private String remediation;

    protected Finding() {
    }

    public static Finding from(RawFinding raw) {
        Finding finding = new Finding();
        finding.ruleId = raw.ruleId();
        finding.title = truncate(raw.title(), 255);
        finding.description = truncate(raw.description(), 2000);
        finding.category = raw.category();
        finding.severity = raw.severity();
        finding.confidence = Math.round(raw.confidence() * 100.0) / 100.0;
        finding.cwe = raw.cwe();
        finding.owasp = truncate(raw.owasp(), 96);
        finding.filePath = truncate(raw.filePath(), 1024);
        finding.lineNumber = raw.lineNumber();
        finding.evidence = truncate(raw.evidence(), 512);
        finding.language = truncate(raw.language(), 64);
        finding.remediation = truncate(raw.remediation(), 1200);
        return finding;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public Long getId() { return id; }
    public String getRuleId() { return ruleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public FindingCategory getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public double getConfidence() { return confidence; }
    public String getCwe() { return cwe; }
    public String getOwasp() { return owasp; }
    public String getFilePath() { return filePath; }
    public int getLineNumber() { return lineNumber; }
    public String getEvidence() { return evidence; }
    public String getLanguage() { return language; }
    public String getRemediation() { return remediation; }
}
