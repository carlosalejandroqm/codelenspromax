package com.codelens.report.service;

import com.codelens.report.client.AnalysisDetailView;
import com.codelens.report.client.AnalysisServiceClient;
import com.codelens.report.client.AnalysisView;
import com.codelens.report.client.FindingView;
import com.codelens.report.model.Recommendation;
import com.codelens.report.model.Report;
import com.codelens.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso "generar reporte": pide el analisis con sus hallazgos, construye
 * el plan de remediacion, redacta el resumen y persiste el documento.
 */
@Service
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    private final AnalysisServiceClient analysisClient;
    private final RecommendationEngine recommendationEngine;
    private final ExecutiveSummaryWriter summaryWriter;
    private final MarkdownReportRenderer renderer;
    private final VerdictResolver verdictResolver;
    private final ReportRepository reportRepository;

    public ReportGenerator(AnalysisServiceClient analysisClient,
                           RecommendationEngine recommendationEngine,
                           ExecutiveSummaryWriter summaryWriter,
                           MarkdownReportRenderer renderer,
                           VerdictResolver verdictResolver,
                           ReportRepository reportRepository) {
        this.analysisClient = analysisClient;
        this.recommendationEngine = recommendationEngine;
        this.summaryWriter = summaryWriter;
        this.renderer = renderer;
        this.verdictResolver = verdictResolver;
        this.reportRepository = reportRepository;
    }

    @Transactional
    public Report generate(String analysisId, String ownerId) {
        AnalysisDetailView detail = analysisClient.fetchDetail(analysisId);
        AnalysisView analysis = detail.analysis();
        List<FindingView> findings = detail.findings() == null ? List.of() : detail.findings();

        if (!"COMPLETED".equalsIgnoreCase(analysis.status())) {
            throw new ReportGenerationException(
                    "El analisis " + analysisId + " esta en estado " + analysis.status()
                            + "; solo se reporta un analisis completado");
        }

        Report report = new Report(UUID.randomUUID().toString(), analysisId, analysis.projectName());
        report.setSourceId(analysis.sourceId());
        report.setOwnerId(ownerId);
        report.setGrade(analysis.grade());
        report.setRiskLevel(analysis.riskLevel());
        report.setOverallScore(analysis.overallScore());
        report.setSecurityScore(analysis.securityScore());
        report.setHealthScore(analysis.healthScore());

        String verdict = verdictResolver.resolve(analysis);
        report.setVerdict(verdict);

        List<Recommendation> recommendations = recommendationEngine.build(analysis, findings);
        recommendations.forEach(report::addRecommendation);

        report.setExecutiveSummary(summaryWriter.write(analysis, findings, recommendations, verdict));
        report.getStrengths().addAll(summaryWriter.strengths(analysis, findings));
        report.getKeyRisks().addAll(summaryWriter.keyRisks(analysis, findings));
        report.setMarkdown(renderer.render(report, analysis, findings));

        log.info("Reporte generado para el analisis {}: veredicto {} con {} recomendaciones",
                analysisId, verdict, recommendations.size());

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Report get(String id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ReportNotFoundException("No existe el reporte " + id));
    }

    /** Con ownerId solo devuelve los reportes de ese usuario. */
    @Transactional(readOnly = true)
    public List<Report> list(String ownerId) {
        return ownerId == null || ownerId.isBlank()
                ? reportRepository.findAllByOrderByGeneratedAtDesc()
                : reportRepository.findByOwnerIdOrderByGeneratedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Report> listByAnalysis(String analysisId) {
        return reportRepository.findByAnalysisIdOrderByGeneratedAtDesc(analysisId);
    }

    @Transactional
    public void delete(String id) {
        reportRepository.delete(get(id));
    }
}
