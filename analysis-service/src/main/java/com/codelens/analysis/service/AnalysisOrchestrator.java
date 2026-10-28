package com.codelens.analysis.service;

import com.codelens.analysis.client.SourceFileView;
import com.codelens.analysis.client.SourceServiceClient;
import com.codelens.analysis.client.SourceSnapshotView;
import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.AnalysisEngine;
import com.codelens.analysis.engine.analyzers.DependencyAnalyzer;
import com.codelens.analysis.engine.analyzers.DuplicationAnalyzer;
import com.codelens.analysis.engine.model.*;
import com.codelens.analysis.model.Analysis;
import com.codelens.analysis.model.AnalysisStatus;
import com.codelens.analysis.model.Finding;
import com.codelens.analysis.model.LanguageStat;
import com.codelens.analysis.repository.AnalysisRepository;
import com.codelens.analysis.repository.FindingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso "analizar un snapshot": pide el codigo al source-service, ejecuta
 * el motor y persiste el resultado agregado junto a los hallazgos.
 */
@Service
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);

    private final SourceServiceClient sourceClient;
    private final AnalysisEngine engine;
    private final AnalysisRepository analysisRepository;
    private final FindingRepository findingRepository;
    private final AnalysisProperties properties;

    public AnalysisOrchestrator(SourceServiceClient sourceClient,
                                AnalysisEngine engine,
                                AnalysisRepository analysisRepository,
                                FindingRepository findingRepository,
                                AnalysisProperties properties) {
        this.sourceClient = sourceClient;
        this.engine = engine;
        this.analysisRepository = analysisRepository;
        this.findingRepository = findingRepository;
        this.properties = properties;
    }

    @Transactional
    public Analysis analyze(String sourceId, String ownerId) {
        SourceSnapshotView snapshot = sourceClient.fetchSnapshot(sourceId);
        if (!snapshot.isReady()) {
            throw new AnalysisFailedException(
                    "El snapshot " + sourceId + " esta en estado " + snapshot.status() + " y no se puede analizar");
        }

        Analysis analysis = new Analysis(UUID.randomUUID().toString(), sourceId, snapshot.projectName());
        analysis.setOwnerId(ownerId);
        analysis.setStatus(AnalysisStatus.RUNNING);

        try {
            List<SourceFileView> files = sourceClient.fetchAnalyzableFiles(sourceId);
            if (files.isEmpty()) {
                throw new AnalysisFailedException("El snapshot no contiene archivos de texto analizables");
            }
            if (files.size() > properties.getMaxFilesPerAnalysis()) {
                log.warn("El snapshot {} tiene {} archivos; se analizaran los primeros {}",
                        sourceId, files.size(), properties.getMaxFilesPerAnalysis());
                files = files.subList(0, properties.getMaxFilesPerAnalysis());
            }

            Map<String, String> contents = sourceClient.fetchContents(sourceId, files);
            if (contents.isEmpty()) {
                throw new AnalysisFailedException("No se pudo descargar el contenido de ningun archivo");
            }

            EngineOutcome outcome = engine.run(snapshot.projectName(), contents);
            applyOutcome(analysis, outcome);
            analysis.markCompleted();
        } catch (AnalysisFailedException e) {
            analysis.markFailed(e.getMessage());
            analysisRepository.save(analysis);
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado analizando {}", sourceId, e);
            analysis.markFailed(e.getMessage());
            analysisRepository.save(analysis);
            throw e;
        }

        return analysisRepository.save(analysis);
    }

    /** Traduce el resultado del motor al agregado persistente. */
    private void applyOutcome(Analysis analysis, EngineOutcome outcome) {
        analysis.setFilesAnalyzed(outcome.filesAnalyzed());
        analysis.setDurationMillis(outcome.durationMillis());

        List<FileMetrics> metrics = outcome.metrics();
        analysis.setTotalLines(metrics.stream().mapToLong(FileMetrics::totalLines).sum());
        analysis.setCodeLines(metrics.stream().mapToLong(FileMetrics::codeLines).sum());
        analysis.setCommentLines(metrics.stream().mapToLong(FileMetrics::commentLines).sum());
        analysis.setBlankLines(metrics.stream().mapToLong(FileMetrics::blankLines).sum());
        analysis.setMaxComplexity(metrics.stream().mapToInt(FileMetrics::maxComplexity).max().orElse(0));
        analysis.setAvgComplexity(round(metrics.stream()
                .filter(m -> m.functionCount() > 0)
                .mapToDouble(FileMetrics::avgComplexity)
                .average().orElse(0)));
        analysis.setTodoCount(metrics.stream().mapToInt(FileMetrics::todoCount).sum());

        long testFiles = metrics.stream().filter(FileMetrics::testFile).count();
        long productionFiles = Math.max(1, metrics.size() - testFiles);
        analysis.setTestFileRatio(round((double) testFiles / productionFiles));

        analysis.setDuplicationRatio(round(outcome.gauges()
                .getOrDefault(DuplicationAnalyzer.GAUGE_RATIO, 0.0)));
        analysis.setDeclaredDependencies((int) Math.round(outcome.gauges()
                .getOrDefault(DependencyAnalyzer.GAUGE_DEPENDENCIES, 0.0)));

        QualityScore score = outcome.score();
        analysis.setSecurityScore(score.securityScore());
        analysis.setHealthScore(score.healthScore());
        analysis.setOverallScore(score.overallScore());
        analysis.setGrade(score.grade());
        analysis.setRiskLevel(score.riskLevel());
        analysis.getSubScores().clear();
        score.subScores().forEach((key, value) ->
                analysis.getSubScores().put(key, Math.round(value * 10.0) / 10.0));

        analysis.setLanguages(outcome.languages().stream()
                .map(l -> new LanguageStat(l.language(), l.files(), l.codeLines(), l.percentage()))
                .toList());

        for (RawFinding raw : outcome.findings()) {
            analysis.addFinding(Finding.from(raw));
        }
        countBySeverity(analysis, outcome.findings());
        countByCategory(analysis, outcome.findings());
    }

    private void countBySeverity(Analysis analysis, List<RawFinding> findings) {
        analysis.setCriticalCount(count(findings, Severity.CRITICAL));
        analysis.setHighCount(count(findings, Severity.HIGH));
        analysis.setMediumCount(count(findings, Severity.MEDIUM));
        analysis.setLowCount(count(findings, Severity.LOW));
        analysis.setInfoCount(count(findings, Severity.INFO));
    }

    private void countByCategory(Analysis analysis, List<RawFinding> findings) {
        analysis.setSecurityFindings(count(findings, FindingCategory.SECURITY));
        analysis.setSecretFindings(count(findings, FindingCategory.SECRET));
        analysis.setDependencyFindings(count(findings, FindingCategory.DEPENDENCY)
                + count(findings, FindingCategory.CONFIGURATION));
        analysis.setMaintainabilityFindings(count(findings, FindingCategory.MAINTAINABILITY));
        analysis.setTechnicalDebtFindings(count(findings, FindingCategory.TECHNICAL_DEBT));
    }

    private int count(List<RawFinding> findings, Severity severity) {
        return (int) findings.stream().filter(f -> f.severity() == severity).count();
    }

    private int count(List<RawFinding> findings, FindingCategory category) {
        return (int) findings.stream().filter(f -> f.category() == category).count();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    @Transactional(readOnly = true)
    public Analysis get(String id) {
        return analysisRepository.findById(id)
                .orElseThrow(() -> new AnalysisNotFoundException("No existe el analisis " + id));
    }

    /** Con ownerId solo devuelve los analisis de ese usuario. */
    @Transactional(readOnly = true)
    public List<Analysis> list(String ownerId) {
        return ownerId == null || ownerId.isBlank()
                ? analysisRepository.findAllByOrderByStartedAtDesc()
                : analysisRepository.findByOwnerIdOrderByStartedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Analysis> listBySource(String sourceId) {
        return analysisRepository.findBySourceIdOrderByStartedAtDesc(sourceId);
    }

    @Transactional(readOnly = true)
    public List<Finding> findings(String analysisId) {
        get(analysisId);
        return findingRepository.findByAnalysisIdOrderBySeverityAscConfidenceDesc(analysisId);
    }

    @Transactional
    public void delete(String id) {
        analysisRepository.delete(get(id));
    }
}
