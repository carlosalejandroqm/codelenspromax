package com.codelens.analysis.model;

import com.codelens.analysis.engine.model.Severity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ejecucion de analisis sobre un snapshot de codigo. Guarda el resumen agregado
 * (scores, conteos, metricas) y los hallazgos individuales, que es lo que el
 * servicio de reportes necesita para redactar recomendaciones.
 */
@Entity
@Table(name = "analysis", indexes = @Index(name = "idx_analysis_source", columnList = "source_id"))
public class Analysis {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "source_id", nullable = false, length = 36)
    private String sourceId;

    /** Usuario que lanzo el analisis (X-User-Id del gateway); null en llamadas directas. */
    @Column(length = 36)
    private String ownerId;

    @Column(nullable = false, length = 255)
    private String projectName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant completedAt;
    private long durationMillis;

    private int filesAnalyzed;
    private long totalLines;
    private long codeLines;
    private long commentLines;
    private long blankLines;

    private double securityScore;
    private double healthScore;
    private double overallScore;

    @Column(length = 2)
    private String grade;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Severity riskLevel;

    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private int infoCount;

    private int securityFindings;
    private int secretFindings;
    private int dependencyFindings;
    private int maintainabilityFindings;
    private int technicalDebtFindings;

    private double duplicationRatio;
    private double avgComplexity;
    private int maxComplexity;
    private double testFileRatio;
    private int todoCount;
    private int declaredDependencies;

    @Column(length = 2000)
    private String errorMessage;

    /** Desglose de los factores de salud, para que el reporte los explique. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_sub_score", joinColumns = @JoinColumn(name = "analysis_id"))
    @MapKeyColumn(name = "factor", length = 64)
    @Column(name = "score")
    private Map<String, Double> subScores = new LinkedHashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_language", joinColumns = @JoinColumn(name = "analysis_id"))
    private List<LanguageStat> languages = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Finding> findings = new ArrayList<>();

    protected Analysis() {
    }

    public Analysis(String id, String sourceId, String projectName) {
        this.id = id;
        this.sourceId = sourceId;
        this.projectName = projectName;
    }

    public void addFinding(Finding finding) {
        finding.setAnalysis(this);
        this.findings.add(finding);
    }

    public void markFailed(String message) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = message != null && message.length() > 1990 ? message.substring(0, 1990) : message;
        this.completedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = AnalysisStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSourceId() { return sourceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }
    public int getFilesAnalyzed() { return filesAnalyzed; }
    public void setFilesAnalyzed(int filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; }
    public long getTotalLines() { return totalLines; }
    public void setTotalLines(long totalLines) { this.totalLines = totalLines; }
    public long getCodeLines() { return codeLines; }
    public void setCodeLines(long codeLines) { this.codeLines = codeLines; }
    public long getCommentLines() { return commentLines; }
    public void setCommentLines(long commentLines) { this.commentLines = commentLines; }
    public long getBlankLines() { return blankLines; }
    public void setBlankLines(long blankLines) { this.blankLines = blankLines; }
    public double getSecurityScore() { return securityScore; }
    public void setSecurityScore(double securityScore) { this.securityScore = securityScore; }
    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Severity getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Severity riskLevel) { this.riskLevel = riskLevel; }
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
    public int getHighCount() { return highCount; }
    public void setHighCount(int highCount) { this.highCount = highCount; }
    public int getMediumCount() { return mediumCount; }
    public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }
    public int getLowCount() { return lowCount; }
    public void setLowCount(int lowCount) { this.lowCount = lowCount; }
    public int getInfoCount() { return infoCount; }
    public void setInfoCount(int infoCount) { this.infoCount = infoCount; }
    public int getSecurityFindings() { return securityFindings; }
    public void setSecurityFindings(int securityFindings) { this.securityFindings = securityFindings; }
    public int getSecretFindings() { return secretFindings; }
    public void setSecretFindings(int secretFindings) { this.secretFindings = secretFindings; }
    public int getDependencyFindings() { return dependencyFindings; }
    public void setDependencyFindings(int dependencyFindings) { this.dependencyFindings = dependencyFindings; }
    public int getMaintainabilityFindings() { return maintainabilityFindings; }
    public void setMaintainabilityFindings(int v) { this.maintainabilityFindings = v; }
    public int getTechnicalDebtFindings() { return technicalDebtFindings; }
    public void setTechnicalDebtFindings(int v) { this.technicalDebtFindings = v; }
    public double getDuplicationRatio() { return duplicationRatio; }
    public void setDuplicationRatio(double duplicationRatio) { this.duplicationRatio = duplicationRatio; }
    public double getAvgComplexity() { return avgComplexity; }
    public void setAvgComplexity(double avgComplexity) { this.avgComplexity = avgComplexity; }
    public int getMaxComplexity() { return maxComplexity; }
    public void setMaxComplexity(int maxComplexity) { this.maxComplexity = maxComplexity; }
    public double getTestFileRatio() { return testFileRatio; }
    public void setTestFileRatio(double testFileRatio) { this.testFileRatio = testFileRatio; }
    public int getTodoCount() { return todoCount; }
    public void setTodoCount(int todoCount) { this.todoCount = todoCount; }
    public int getDeclaredDependencies() { return declaredDependencies; }
    public void setDeclaredDependencies(int declaredDependencies) { this.declaredDependencies = declaredDependencies; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Double> getSubScores() { return subScores; }
    public void setSubScores(Map<String, Double> subScores) { this.subScores = subScores; }
    public List<LanguageStat> getLanguages() { return languages; }
    public void setLanguages(List<LanguageStat> languages) { this.languages = languages; }
    public List<Finding> getFindings() { return findings; }
}
