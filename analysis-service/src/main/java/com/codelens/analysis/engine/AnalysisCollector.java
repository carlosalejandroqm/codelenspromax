package com.codelens.analysis.engine;

import com.codelens.analysis.engine.model.FileMetrics;
import com.codelens.analysis.engine.model.RawFinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acumulador compartido por todos los analizadores. Aplica el tope de hallazgos
 * por regla y archivo para que un patron muy frecuente no sepulte el reporte.
 */
public class AnalysisCollector {

    private final List<RawFinding> findings = new ArrayList<>();
    private final List<FileMetrics> metrics = new ArrayList<>();
    private final Map<String, Integer> perRuleAndFile = new HashMap<>();
    private final Map<String, Integer> suppressed = new HashMap<>();
    private final Map<String, Double> gauges = new HashMap<>();
    private final int maxFindingsPerRulePerFile;

    public AnalysisCollector(int maxFindingsPerRulePerFile) {
        this.maxFindingsPerRulePerFile = maxFindingsPerRulePerFile;
    }

    /** @return true si el hallazgo se acepto; false si se recorto por el tope. */
    public boolean add(RawFinding finding) {
        String key = finding.ruleId() + "@" + (finding.filePath() == null ? "-" : finding.filePath());
        int current = perRuleAndFile.getOrDefault(key, 0);
        if (current >= maxFindingsPerRulePerFile) {
            suppressed.merge(finding.ruleId(), 1, Integer::sum);
            return false;
        }
        perRuleAndFile.put(key, current + 1);
        findings.add(finding);
        return true;
    }

    public void addMetrics(FileMetrics fileMetrics) {
        metrics.add(fileMetrics);
    }

    public List<RawFinding> findings() {
        return Collections.unmodifiableList(findings);
    }

    public List<FileMetrics> metrics() {
        return Collections.unmodifiableList(metrics);
    }

    /**
     * Canal para metricas numericas que un analizador calcula y otro componente
     * necesita (por ejemplo el ratio de duplicacion que consume el score).
     */
    public void putGauge(String name, double value) {
        gauges.put(name, value);
    }

    public double gauge(String name, double defaultValue) {
        return gauges.getOrDefault(name, defaultValue);
    }

    public Map<String, Double> gauges() {
        return Collections.unmodifiableMap(gauges);
    }

    /** Hallazgos omitidos por regla, para poder avisar en el reporte. */
    public Map<String, Integer> suppressedByRule() {
        return Collections.unmodifiableMap(suppressed);
    }
}
