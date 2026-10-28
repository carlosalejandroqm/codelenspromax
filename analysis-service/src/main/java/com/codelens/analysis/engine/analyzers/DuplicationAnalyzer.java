package com.codelens.analysis.engine.analyzers;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.*;
import com.codelens.analysis.engine.spi.ProjectAnalyzer;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Detector de codigo duplicado por huella de ventanas deslizantes.
 *
 * Normaliza cada linea (espacios colapsados, minusculas) y calcula el hash de
 * bloques de N lineas consecutivas; dos bloques con el mismo hash en sitios
 * distintos son un clon. Al trabajar sobre texto normalizado y no sobre un AST,
 * funciona para cualquier lenguaje, incluidos los que el motor no conoce.
 */
@Component
public class DuplicationAnalyzer implements ProjectAnalyzer {

    /** Nombre del gauge que publica el ratio de duplicacion al calculo de score. */
    public static final String GAUGE_RATIO = "duplication.ratio";

    private static final int MIN_MEANINGFUL_CHARS = 12;
    private static final int MAX_REPORTED_CLONES = 40;

    private final AnalysisProperties properties;

    public DuplicationAnalyzer(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "duplication";
    }

    @Override
    public void analyze(ProjectContext context, AnalysisCollector collector) {
        int window = Math.max(3, properties.getDuplicationWindowLines());
        Map<String, List<Location>> blocks = new HashMap<>();
        Map<String, Set<Integer>> duplicatedLinesByFile = new HashMap<>();
        long totalCodeLines = 0;

        for (AnalysisUnit unit : context.units()) {
            List<String> normalized = normalize(unit);
            totalCodeLines += normalized.stream().filter(l -> !l.isBlank()).count();

            for (int i = 0; i + window <= normalized.size(); i++) {
                List<String> slice = normalized.subList(i, i + window);
                if (!isMeaningful(slice)) {
                    continue;
                }
                String fingerprint = String.join("\n", slice);
                blocks.computeIfAbsent(fingerprint, k -> new ArrayList<>())
                        .add(new Location(unit.path(), i + 1, unit.languageName()));
            }
        }

        int reported = 0;
        Map<String, Set<Integer>> alreadyReported = new HashMap<>();

        // Ordenar por posicion hace que de un clon largo se reporte su inicio y
        // se descarten las ventanas solapadas que lo siguen.
        List<Map.Entry<String, List<Location>>> clones = blocks.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .sorted(Comparator
                        .comparing((Map.Entry<String, List<Location>> e) -> e.getValue().get(0).path())
                        .thenComparingInt(e -> e.getValue().get(0).startLine()))
                .toList();

        for (var entry : clones) {
            List<Location> locations = entry.getValue();

            for (Location location : locations) {
                Set<Integer> lines = duplicatedLinesByFile
                        .computeIfAbsent(location.path(), k -> new HashSet<>());
                for (int offset = 0; offset < window; offset++) {
                    lines.add(location.startLine() + offset);
                }
            }

            Location first = locations.get(0);
            if (overlapsReported(alreadyReported, first, window)) {
                continue;
            }
            markReported(alreadyReported, locations, window);

            if (reported++ >= MAX_REPORTED_CLONES) {
                continue;
            }
            String others = locations.stream().skip(1).limit(4)
                    .map(l -> l.path() + ":" + l.startLine())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            collector.add(RawFinding.builder("HEALTH-DUPLICATION")
                    .title("Bloque de codigo duplicado")
                    .description(("Un bloque de %d lineas aparece %d veces en el proyecto. Tambien en: %s. "
                            + "El codigo duplicado obliga a corregir el mismo defecto varias veces.")
                            .formatted(window, locations.size(), others))
                    .category(FindingCategory.MAINTAINABILITY)
                    .severity(locations.size() >= 4 ? Severity.MEDIUM : Severity.LOW)
                    .confidence(0.75)
                    .filePath(first.path())
                    .lineNumber(first.startLine())
                    .evidence(entry.getKey().lines().findFirst().orElse(""))
                    .language(first.language())
                    .remediation("Extrae el bloque a una funcion, modulo o componente compartido y reemplaza "
                            + "cada copia por una llamada.")
                    .build());
        }

        long duplicatedLines = duplicatedLinesByFile.values().stream().mapToLong(Set::size).sum();
        double ratio = totalCodeLines == 0 ? 0 : (double) duplicatedLines / totalCodeLines;
        collector.putGauge(GAUGE_RATIO, Math.min(1.0, ratio));
    }

    /** true si este clon cae dentro de un bloque duplicado ya reportado. */
    private boolean overlapsReported(Map<String, Set<Integer>> reported, Location location, int window) {
        Set<Integer> lines = reported.get(location.path());
        if (lines == null) {
            return false;
        }
        for (int offset = 0; offset < window; offset++) {
            if (lines.contains(location.startLine() + offset)) {
                return true;
            }
        }
        return false;
    }

    private void markReported(Map<String, Set<Integer>> reported, List<Location> locations, int window) {
        for (Location location : locations) {
            Set<Integer> lines = reported.computeIfAbsent(location.path(), k -> new HashSet<>());
            for (int offset = 0; offset < window; offset++) {
                lines.add(location.startLine() + offset);
            }
        }
    }

    /** Lineas de codigo sin comentarios, con espacios colapsados. */
    private List<String> normalize(AnalysisUnit unit) {
        return unit.codeLines().stream()
                .map(line -> line.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * Descarta ventanas triviales (imports, cierres de bloque, lineas cortas)
     * que aparecerian duplicadas en cualquier proyecto sin significar nada.
     */
    private boolean isMeaningful(List<String> slice) {
        int substantive = 0;
        for (String line : slice) {
            if (line.length() >= MIN_MEANINGFUL_CHARS && !line.matches("^[\\s{}()\\[\\];,]*$")) {
                substantive++;
            }
        }
        return substantive >= slice.size() - 1 && substantive >= 3;
    }

    private record Location(String path, int startLine, String language) {
    }
}
