package com.codelens.analysis.engine;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.model.*;
import com.codelens.analysis.engine.spi.FileAnalyzer;
import com.codelens.analysis.engine.spi.ProjectAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestador del analisis.
 *
 * El pipeline es fijo y el conocimiento es enchufable: normaliza cada archivo,
 * pasa todos los {@link FileAnalyzer} sobre el, luego todos los
 * {@link ProjectAnalyzer} sobre el conjunto y finalmente calcula las notas.
 * Spring inyecta las dos listas, asi que un analizador nuevo entra al pipeline
 * solo por existir como bean.
 */
@Component
public class AnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(AnalysisEngine.class);

    private final LanguageRegistry languageRegistry;
    private final SourceNormalizer normalizer;
    private final ScoreCalculator scoreCalculator;
    private final List<FileAnalyzer> fileAnalyzers;
    private final List<ProjectAnalyzer> projectAnalyzers;
    private final AnalysisProperties properties;

    public AnalysisEngine(LanguageRegistry languageRegistry,
                          SourceNormalizer normalizer,
                          ScoreCalculator scoreCalculator,
                          List<FileAnalyzer> fileAnalyzers,
                          List<ProjectAnalyzer> projectAnalyzers,
                          AnalysisProperties properties) {
        this.languageRegistry = languageRegistry;
        this.normalizer = normalizer;
        this.scoreCalculator = scoreCalculator;
        this.fileAnalyzers = fileAnalyzers;
        this.projectAnalyzers = projectAnalyzers;
        this.properties = properties;
    }

    /**
     * @param projectName nombre del proyecto, solo informativo
     * @param sources     contenido de cada archivo indexado por ruta relativa
     */
    public EngineOutcome run(String projectName, Map<String, String> sources) {
        long startedAt = System.currentTimeMillis();
        AnalysisCollector collector = new AnalysisCollector(properties.getMaxFindingsPerRulePerFile());

        List<AnalysisUnit> units = new ArrayList<>(sources.size());
        for (var entry : sources.entrySet()) {
            LanguageProfile profile = languageRegistry.resolve(entry.getKey());
            AnalysisUnit unit = normalizer.normalize(entry.getKey(), entry.getValue(), profile);
            units.add(unit);

            for (FileAnalyzer analyzer : fileAnalyzers) {
                if (!analyzer.supports(unit)) {
                    continue;
                }
                try {
                    analyzer.analyze(unit, collector);
                } catch (RuntimeException e) {
                    // Un analizador que falla no puede tumbar el analisis completo.
                    log.warn("El analizador {} fallo en {}: {}", analyzer.id(), unit.path(), e.toString());
                }
            }
        }

        ProjectContext context = new ProjectContext(projectName, units);
        for (ProjectAnalyzer analyzer : projectAnalyzers) {
            try {
                analyzer.analyze(context, collector);
            } catch (RuntimeException e) {
                log.warn("El analizador de proyecto {} fallo: {}", analyzer.id(), e.toString());
            }
        }

        List<RawFinding> findings = sortByRelevance(collector.findings());
        List<LanguageUsage> languages = languageBreakdown(collector.metrics());
        QualityScore score = scoreCalculator.calculate(findings, collector.metrics(), collector.gauges());
        long duration = System.currentTimeMillis() - startedAt;

        log.info("Analisis de '{}' terminado: {} archivos, {} hallazgos, score {} ({}) en {} ms",
                projectName, units.size(), findings.size(), score.overallScore(), score.grade(), duration);

        return new EngineOutcome(findings, collector.metrics(), languages, score,
                collector.gauges(), collector.suppressedByRule(), units.size(), duration);
    }

    /** Primero lo mas grave y, a igual gravedad, lo mas fiable. */
    private List<RawFinding> sortByRelevance(List<RawFinding> findings) {
        return findings.stream()
                .sorted(Comparator
                        .comparingInt((RawFinding f) -> f.severity().getRank()).reversed()
                        .thenComparing(Comparator.comparingDouble(RawFinding::confidence).reversed())
                        .thenComparing(f -> f.filePath() == null ? "" : f.filePath())
                        .thenComparingInt(RawFinding::lineNumber))
                .toList();
    }

    private List<LanguageUsage> languageBreakdown(List<FileMetrics> metrics) {
        long totalCodeLines = metrics.stream().mapToLong(FileMetrics::codeLines).sum();
        Map<String, List<FileMetrics>> grouped = metrics.stream()
                .collect(Collectors.groupingBy(FileMetrics::language, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    long lines = entry.getValue().stream().mapToLong(FileMetrics::codeLines).sum();
                    double percentage = totalCodeLines == 0 ? 0
                            : Math.round(lines * 1000.0 / totalCodeLines) / 10.0;
                    return new LanguageUsage(entry.getKey(), entry.getValue().size(), lines, percentage);
                })
                .sorted(Comparator.comparingLong(LanguageUsage::codeLines).reversed())
                .toList();
    }

    /** Analizadores activos, para exponerlos como metadato del analisis. */
    public List<String> activeAnalyzers() {
        return java.util.stream.Stream.concat(
                        fileAnalyzers.stream().map(FileAnalyzer::id),
                        projectAnalyzers.stream().map(ProjectAnalyzer::id))
                .sorted()
                .collect(Collectors.toList());
    }

    /** Se expone para que el controlador pueda describir el motor. */
    public Function<String, LanguageProfile> languageResolver() {
        return languageRegistry::resolve;
    }
}
