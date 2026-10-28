package com.codelens.report.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte generado a partir de un analisis: veredicto, resumen ejecutivo, plan
 * de remediacion y el documento en Markdown listo para exportar.
 */
@Entity
@Table(name = "report", indexes = @Index(name = "idx_report_analysis", columnList = "analysis_id"))
public class Report {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @Column(name = "source_id", length = 36)
    private String sourceId;

    /** Usuario que genero el reporte (X-User-Id del gateway); null en llamadas directas. */
    @Column(length = 36)
    private String ownerId;

    @Column(nullable = false, length = 255)
    private String projectName;

    @Column(nullable = false)
    private Instant generatedAt = Instant.now();

    /** APROBADO / APROBADO_CON_RESERVAS / RECHAZADO. */
    @Column(nullable = false, length = 32)
    private String verdict;

    @Column(length = 32)
    private String riskLevel;

    @Column(length = 2)
    private String grade;

    private double overallScore;
    private double securityScore;
    private double healthScore;

    @Lob
    @Column(length = 8000)
    private String executiveSummary;

    /** Puntos fuertes detectados: un reporte solo con fallos no se usa. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_strength", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "text", length = 500)
    private List<String> strengths = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_risk", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "text", length = 500)
    private List<String> keyRisks = new ArrayList<>();

    @Lob
    private String markdown;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("rank ASC")
    private List<Recommendation> recommendations = new ArrayList<>();

    protected Report() {
    }

    public Report(String id, String analysisId, String projectName) {
        this.id = id;
        this.analysisId = analysisId;
        this.projectName = projectName;
    }

    public void addRecommendation(Recommendation recommendation) {
        recommendation.setReport(this);
        recommendation.setRank(this.recommendations.size() + 1);
        this.recommendations.add(recommendation);
    }

    public String getId() { return id; }
    public String getAnalysisId() { return analysisId; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getProjectName() { return projectName; }
    public Instant getGeneratedAt() { return generatedAt; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public double getSecurityScore() { return securityScore; }
    public void setSecurityScore(double securityScore) { this.securityScore = securityScore; }
    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getKeyRisks() { return keyRisks; }
    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }
    public List<Recommendation> getRecommendations() { return recommendations; }
}
