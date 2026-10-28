package com.codelens.analysis.engine;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.analyzers.DuplicationAnalyzer;
import com.codelens.analysis.engine.model.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte hallazgos y metricas en las notas que ve el usuario.
 *
 * Dos ideas guian el calculo:
 * <ul>
 *   <li>La penalizacion de seguridad se normaliza por tamano del proyecto, para
 *       que un monorepo no salga peor que un script por el simple hecho de tener
 *       mas codigo.</li>
 *   <li>La nota de salud es la media ponderada de factores explicables
 *       (documentacion, complejidad, duplicacion, tamano, pruebas, deuda), asi
 *       el reporte puede justificar de donde sale cada punto perdido.</li>
 * </ul>
 */
@Component
public class ScoreCalculator {

    private static final double SECURITY_WEIGHT = 0.60;
    private static final double HEALTH_WEIGHT = 0.40;
    private static final double SECURITY_DECAY = 60.0;

    private final AnalysisProperties properties;

    public ScoreCalculator(AnalysisProperties properties) {
        this.properties = properties;
    }

    public QualityScore calculate(List<RawFinding> findings, List<FileMetrics> metrics,
                                  Map<String, Double> gauges) {
        long codeLines = metrics.stream().mapToLong(FileMetrics::codeLines).sum();
        double kilolines = Math.max(0.2, codeLines / 1000.0);

        double security = securityScore(findings, kilolines);
        Map<String, Double> subScores = healthSubScores(findings, metrics, gauges, codeLines);
        double health = subScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
        double overall = security * SECURITY_WEIGHT + health * HEALTH_WEIGHT;

        return new QualityScore(round(security), round(health), round(overall),
                grade(overall), riskLevel(findings), subScores);
    }

    /** Penalizacion acumulada, ponderada por confianza y amortiguada por tamano. */
    private double securityScore(List<RawFinding> findings, double kilolines) {
        double penalty = findings.stream()
                .filter(this::countsForSecurity)
                .mapToDouble(f -> f.severity().getPenalty() * f.confidence())
                .sum();
        double normalized = penalty / Math.sqrt(kilolines);
        return clamp(100.0 * Math.exp(-normalized / SECURITY_DECAY));
    }

    private boolean countsForSecurity(RawFinding finding) {
        return switch (finding.category()) {
            case SECURITY, SECRET, DEPENDENCY, CONFIGURATION -> true;
            case MAINTAINABILITY, TECHNICAL_DEBT -> false;
        };
    }

    private Map<String, Double> healthSubScores(List<RawFinding> findings, List<FileMetrics> metrics,
                                                Map<String, Double> gauges, long codeLines) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (metrics.isEmpty()) {
            scores.put("documentacion", 100.0);
            return scores;
        }

        long commentLines = metrics.stream().mapToLong(FileMetrics::commentLines).sum();
        double commentRatio = codeLines == 0 ? 0 : (double) commentLines / codeLines;
        scores.put("documentacion", documentationScore(commentRatio));

        double avgMaxComplexity = metrics.stream()
                .filter(m -> m.functionCount() > 0)
                .mapToInt(FileMetrics::maxComplexity)
                .average().orElse(1.0);
        scores.put("complejidad", clamp(100.0 - Math.max(0,
                avgMaxComplexity - properties.getComplexityThreshold() / 2.0) * 6.0));

        double duplicationRatio = gauges.getOrDefault(DuplicationAnalyzer.GAUGE_RATIO, 0.0);
        scores.put("duplicacion", clamp(100.0 - duplicationRatio * 250.0));

        long oversizedFiles = metrics.stream()
                .filter(m -> m.totalLines() > properties.getLongFileThreshold())
                .count();
        scores.put("tamano_archivos", clamp(100.0 - (double) oversizedFiles / metrics.size() * 200.0));

        long testFiles = metrics.stream().filter(FileMetrics::testFile).count();
        long productionFiles = Math.max(1, metrics.size() - testFiles);
        double testRatio = (double) testFiles / productionFiles;
        scores.put("pruebas", clamp(Math.min(1.0, testRatio / 0.25) * 100.0));

        long debtMarkers = findings.stream()
                .filter(f -> f.category() == FindingCategory.TECHNICAL_DEBT)
                .count();
        double debtPerKilo = codeLines == 0 ? 0 : debtMarkers / Math.max(0.2, codeLines / 1000.0);
        scores.put("deuda_declarada", clamp(100.0 - debtPerKilo * 8.0));

        double avgNesting = metrics.stream().mapToInt(FileMetrics::maxNestingDepth).average().orElse(0);
        scores.put("anidamiento", clamp(100.0 - Math.max(0, avgNesting - 3) * 15.0));

        return scores;
    }

    /**
     * Penaliza tanto la falta de comentarios como el exceso: por debajo del 5%
     * el codigo suele ser opaco, por encima del 40% hay ruido o codigo muerto
     * comentado.
     */
    private double documentationScore(double commentRatio) {
        if (commentRatio >= 0.08 && commentRatio <= 0.40) {
            return 100.0;
        }
        if (commentRatio < 0.08) {
            return clamp(commentRatio / 0.08 * 100.0);
        }
        return clamp(100.0 - (commentRatio - 0.40) * 120.0);
    }

    private Severity riskLevel(List<RawFinding> findings) {
        return findings.stream()
                .filter(f -> countsForSecurity(f) && f.confidence() >= 0.5)
                .map(RawFinding::severity)
                .max((a, b) -> Integer.compare(a.getRank(), b.getRank()))
                .orElse(Severity.INFO);
    }

    private String grade(double overall) {
        if (overall >= 90) return "A";
        if (overall >= 80) return "B";
        if (overall >= 70) return "C";
        if (overall >= 60) return "D";
        if (overall >= 45) return "E";
        return "F";
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
