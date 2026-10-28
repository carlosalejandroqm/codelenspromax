package com.codelens.analysis.engine.analyzers;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.*;
import com.codelens.analysis.engine.spi.FileAnalyzer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Metricas de salud y mantenibilidad, calculadas de forma agnostica al lenguaje.
 *
 * Todo lo especifico del lenguaje se lee del {@link LanguageProfile}: las
 * palabras que ramifican el flujo, los patrones que abren una funcion y si los
 * bloques se delimitan con llaves o con indentacion. Un lenguaje desconocido usa
 * el perfil generico y obtiene metricas aproximadas en lugar de ninguna.
 *
 * La deteccion de limites de funcion es heuristica (del inicio de una funcion al
 * inicio de la siguiente), suficiente para senalar puntos calientes sin pagar el
 * coste de un parser por lenguaje.
 */
@Component
public class CodeHealthAnalyzer implements FileAnalyzer {

    private static final Pattern TODO_MARKER =
            Pattern.compile("(?i)\\b(todo|fixme|hack|xxx|deprecated|workaround)\\b");
    private static final Pattern LOGICAL_OPERATORS = Pattern.compile("&&|\\|\\||\\?\\s*[^:]|\\bcoalesce\\b");
    private static final int TAB_WIDTH = 4;
    private static final int INDENT_UNIT = 4;

    private final AnalysisProperties properties;

    public CodeHealthAnalyzer(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String id() {
        return "code-health";
    }

    @Override
    public void analyze(AnalysisUnit unit, AnalysisCollector collector) {
        LanguageProfile profile = unit.profile();
        List<String> structure = unit.structureLines();

        int blank = 0;
        int commentOnly = 0;
        int code = 0;
        int todos = 0;

        for (int i = 0; i < unit.rawLines().size(); i++) {
            boolean hasCode = !structure.get(i).isBlank();
            boolean hasComment = !unit.commentLines().get(i).isBlank();
            if (hasComment) {
                if (TODO_MARKER.matcher(unit.commentLines().get(i)).find()) {
                    todos++;
                }
                if (!hasCode) {
                    commentOnly++;
                }
            }
            if (hasCode) {
                code++;
            } else if (!hasComment) {
                blank++;
            }
        }

        List<FunctionSpan> functions = detectFunctions(unit, profile);
        int maxComplexity = 0;
        double totalComplexity = 0;
        int longestFunction = 0;

        for (FunctionSpan function : functions) {
            int complexity = cyclomaticComplexity(structure, function.startLine(), function.endLine(), profile);
            int length = function.endLine() - function.startLine() + 1;
            maxComplexity = Math.max(maxComplexity, complexity);
            longestFunction = Math.max(longestFunction, length);
            totalComplexity += complexity;

            if (complexity >= properties.getComplexityThreshold()) {
                collector.add(RawFinding.builder("HEALTH-COMPLEXITY")
                        .title("Funcion con complejidad ciclomatica alta")
                        .description(("La funcion que empieza en la linea %d tiene una complejidad estimada de %d "
                                + "(umbral %d). Cada rama adicional multiplica los caminos a probar y la "
                                + "probabilidad de error.")
                                .formatted(function.startLine(), complexity, properties.getComplexityThreshold()))
                        .category(FindingCategory.MAINTAINABILITY)
                        .severity(complexity >= properties.getComplexityThreshold() * 2
                                ? Severity.HIGH : Severity.MEDIUM)
                        .confidence(0.8)
                        .filePath(unit.path())
                        .lineNumber(function.startLine())
                        .evidence(unit.evidenceAt(function.startLine()))
                        .language(unit.languageName())
                        .remediation("Extrae las ramas en funciones con nombre propio, aplica clausulas de guarda "
                                + "para el retorno temprano y sustituye cadenas de condicionales por tablas de "
                                + "decision o polimorfismo.")
                        .build());
            }

            if (length > properties.getLongFileThreshold() / 4) {
                collector.add(RawFinding.builder("HEALTH-LONG-FUNCTION")
                        .title("Funcion demasiado larga")
                        .description("La funcion que empieza en la linea %d ocupa %d lineas, lo que dificulta "
                                .formatted(function.startLine(), length)
                                + "leerla completa y probarla de forma aislada.")
                        .category(FindingCategory.MAINTAINABILITY)
                        .severity(Severity.LOW)
                        .confidence(0.7)
                        .filePath(unit.path())
                        .lineNumber(function.startLine())
                        .evidence(unit.evidenceAt(function.startLine()))
                        .language(unit.languageName())
                        .remediation("Divide la funcion en pasos con responsabilidad unica y nombre descriptivo.")
                        .build());
            }
        }

        int nesting = maxNestingDepth(structure, profile);
        if (nesting >= 5) {
            collector.add(RawFinding.builder("HEALTH-DEEP-NESTING")
                    .title("Anidamiento excesivo")
                    .description("El archivo alcanza %d niveles de anidamiento. Mas de cuatro niveles suele "
                            .formatted(nesting)
                            + "indicar logica que pide extraccion a funciones intermedias.")
                    .category(FindingCategory.MAINTAINABILITY)
                    .severity(Severity.LOW)
                    .confidence(0.65)
                    .filePath(unit.path())
                    .lineNumber(0)
                    .language(unit.languageName())
                    .remediation("Invierte condiciones y retorna pronto; extrae los bloques internos.")
                    .build());
        }

        if (unit.lineCount() > properties.getLongFileThreshold() && !unit.testFile()) {
            collector.add(RawFinding.builder("HEALTH-LONG-FILE")
                    .title("Archivo demasiado largo")
                    .description("El archivo tiene %d lineas (umbral %d). Los archivos grandes concentran "
                            .formatted(unit.lineCount(), properties.getLongFileThreshold())
                            + "responsabilidades y generan conflictos de merge frecuentes.")
                    .category(FindingCategory.MAINTAINABILITY)
                    .severity(unit.lineCount() > properties.getLongFileThreshold() * 3
                            ? Severity.MEDIUM : Severity.LOW)
                    .confidence(0.9)
                    .filePath(unit.path())
                    .lineNumber(0)
                    .language(unit.languageName())
                    .remediation("Separa el archivo por responsabilidad; agrupa lo que cambia junto.")
                    .build());
        }

        collector.addMetrics(new FileMetrics(
                unit.path(), unit.languageName(), unit.lineCount(), code, commentOnly, blank,
                functions.size(), maxComplexity,
                functions.isEmpty() ? 0 : totalComplexity / functions.size(),
                nesting, longestFunction, todos, unit.testFile()));
    }

    /** Localiza inicios de funcion con los patrones del perfil. */
    private List<FunctionSpan> detectFunctions(AnalysisUnit unit, LanguageProfile profile) {
        List<Pattern> patterns = profile.functionPatterns().stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                .toList();
        if (patterns.isEmpty()) {
            return List.of();
        }

        List<Integer> starts = new ArrayList<>();
        List<String> structure = unit.structureLines();
        for (int i = 0; i < structure.size(); i++) {
            String line = structure.get(i);
            if (line.isBlank()) {
                continue;
            }
            for (Pattern pattern : patterns) {
                if (pattern.matcher(line).find()) {
                    starts.add(i + 1);
                    break;
                }
            }
        }

        List<FunctionSpan> spans = new ArrayList<>(starts.size());
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) - 1 : structure.size();
            spans.add(new FunctionSpan(start, Math.max(start, end)));
        }
        return spans;
    }

    /**
     * Complejidad ciclomatica aproximada: 1 + ramas. Las palabras clave que
     * cuentan como rama las aporta el perfil del lenguaje.
     */
    private int cyclomaticComplexity(List<String> structure, int fromLine, int toLine, LanguageProfile profile) {
        int complexity = 1;
        List<Pattern> keywords = profile.branchKeywords().stream()
                .map(k -> Pattern.compile("\\b" + Pattern.quote(k) + "\\b", Pattern.CASE_INSENSITIVE))
                .toList();

        for (int i = fromLine - 1; i < toLine && i < structure.size(); i++) {
            String line = structure.get(i);
            if (line.isBlank()) {
                continue;
            }
            for (Pattern keyword : keywords) {
                var matcher = keyword.matcher(line);
                while (matcher.find()) {
                    complexity++;
                }
            }
            var operators = LOGICAL_OPERATORS.matcher(line);
            while (operators.find()) {
                complexity++;
            }
        }
        return complexity;
    }

    /** Profundidad maxima por llaves o por indentacion, segun el perfil. */
    private int maxNestingDepth(List<String> structure, LanguageProfile profile) {
        if (profile.blockStyle() == LanguageProfile.BlockStyle.INDENT) {
            int max = 0;
            for (String line : structure) {
                if (line.isBlank()) {
                    continue;
                }
                max = Math.max(max, indentWidth(line) / INDENT_UNIT);
            }
            return max;
        }

        int depth = 0;
        int max = 0;
        for (String line : structure) {
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    depth++;
                    max = Math.max(max, depth);
                } else if (c == '}') {
                    depth = Math.max(0, depth - 1);
                }
            }
        }
        return max;
    }

    private int indentWidth(String line) {
        int width = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += TAB_WIDTH;
            } else {
                break;
            }
        }
        return width;
    }

    /** Rango de lineas (1-based, inclusivo) atribuido a una funcion. */
    private record FunctionSpan(int startLine, int endLine) {
    }
}
