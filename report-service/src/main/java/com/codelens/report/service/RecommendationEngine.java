package com.codelens.report.service;

import com.codelens.report.client.AnalysisView;
import com.codelens.report.client.FindingView;
import com.codelens.report.config.ReportProperties;
import com.codelens.report.model.Effort;
import com.codelens.report.model.Priority;
import com.codelens.report.model.Recommendation;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convierte hallazgos en un plan de trabajo priorizado.
 *
 * Tres decisiones dan forma al resultado:
 * <ol>
 *   <li>Se agrupa por regla, no por hallazgo: 40 concatenaciones de SQL son una
 *       sola tarea ("parametrizar las consultas"), no 40 tareas.</li>
 *   <li>La prioridad combina gravedad y volumen: un problema medio repetido en
 *       todo el proyecto escala por encima de uno grave y aislado.</li>
 *   <li>Ademas de los hallazgos puntuales se anaden recomendaciones sistemicas
 *       derivadas de las metricas de salud (pruebas, duplicacion, documentacion),
 *       que no vienen de ninguna linea concreta.</li>
 * </ol>
 */
@Component
public class RecommendationEngine {

    private final PlaybookCatalog playbooks;
    private final ReportProperties properties;

    public RecommendationEngine(PlaybookCatalog playbooks, ReportProperties properties) {
        this.playbooks = playbooks;
        this.properties = properties;
    }

    public List<Recommendation> build(AnalysisView analysis, List<FindingView> findings) {
        List<FindingView> relevant = findings.stream()
                .filter(f -> f.confidence() >= properties.getMinConfidence())
                .toList();

        // La clave de agrupacion es el playbook cuando hay uno; si no, la regla.
        Map<String, List<FindingView>> grouped = relevant.stream()
                .collect(Collectors.groupingBy(this::groupKey, LinkedHashMap::new, Collectors.toList()));

        List<Scored> scored = new ArrayList<>();
        grouped.forEach((key, group) -> scored.add(fromGroup(group)));
        scored.addAll(systemicRecommendations(analysis));

        return scored.stream()
                .sorted(Comparator
                        .comparingInt((Scored s) -> s.priority().ordinal())
                        .thenComparing(Comparator.comparingDouble(Scored::weight).reversed()))
                .limit(properties.getMaxRecommendations())
                .map(Scored::recommendation)
                .toList();
    }

    /**
     * Agrupa por el patron del playbook que cubre la regla, de forma que varias
     * reglas con la misma remediacion produzcan una sola tarea.
     */
    private String groupKey(FindingView finding) {
        String ruleId = finding.ruleId() == null ? "SIN-REGLA" : finding.ruleId();
        return playbooks.forRule(ruleId)
                .map(playbook -> "playbook:" + playbook.ruleMatch())
                .orElse("rule:" + ruleId);
    }

    private Scored fromGroup(List<FindingView> group) {
        FindingView worst = group.stream()
                .max(Comparator.comparingInt(FindingView::severityRank)
                        .thenComparingDouble(FindingView::confidence))
                .orElseThrow();

        int affectedFiles = (int) group.stream()
                .map(FindingView::filePath)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Optional<Playbook> playbook = playbooks.forRule(worst.ruleId());
        Priority priority = playbook.map(Playbook::priority)
                .filter(p -> p != null && !p.isBlank())
                .map(this::parsePriority)
                .orElseGet(() -> derivePriority(worst, group.size()));

        Effort effort = playbook.map(Playbook::effort)
                .filter(e -> e != null && !e.isBlank())
                .map(this::parseEffort)
                .orElseGet(() -> deriveEffort(group.size(), affectedFiles));

        String title = playbook.map(Playbook::title).filter(t -> t != null && !t.isBlank())
                .orElse(worst.title());

        List<String> ruleIds = group.stream()
                .map(FindingView::ruleId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        String problem = "%d hallazgo(s) en %d archivo(s), detectados por %s. %s"
                .formatted(group.size(), Math.max(affectedFiles, 1),
                        ruleIds.size() == 1 ? "la regla " + ruleIds.get(0)
                                : ruleIds.size() + " reglas (" + String.join(", ", ruleIds) + ")",
                        worst.description() == null ? "" : worst.description());

        String action = playbook.map(Playbook::action).filter(a -> a != null && !a.isBlank())
                .orElse(worst.remediation() != null ? worst.remediation()
                        : "Revisa cada ubicacion listada y corrige el patron detectado.");

        String rationale = playbook.map(Playbook::rationale).filter(r -> r != null && !r.isBlank())
                .orElseGet(() -> defaultRationale(worst, group.size()));

        Recommendation recommendation = new Recommendation(priority, effort, title,
                truncate(problem, 1500), truncate(action, 1500), truncate(rationale, 1500),
                worst.category());
        recommendation.setCwe(worst.cwe());
        recommendation.setOwasp(worst.owasp());
        recommendation.setOwnerRole(playbook.map(Playbook::ownerRole).orElse(defaultOwner(worst.category())));
        recommendation.setFindingsCount(group.size());
        recommendation.setAffectedFiles(affectedFiles);
        recommendation.getRuleIds().addAll(ruleIds);
        group.stream()
                .sorted(Comparator.comparingInt(FindingView::severityRank).reversed())
                .map(FindingView::location)
                .distinct()
                .limit(properties.getExamplesPerRecommendation())
                .forEach(recommendation.getExamples()::add);
        playbook.map(Playbook::references).orElse(List.of())
                .forEach(recommendation.getReferences()::add);

        double weight = worst.severityRank() * worst.confidence() * (1 + Math.log10(group.size() + 1));
        return new Scored(recommendation, priority, weight);
    }

    /**
     * La gravedad marca el suelo y el volumen puede subir un nivel: un problema
     * medio presente en 20 sitios merece mas atencion que uno aislado.
     */
    private Priority derivePriority(FindingView worst, int occurrences) {
        Priority base = switch (worst.severityRank()) {
            case 10 -> Priority.P0;
            case 8 -> Priority.P1;
            case 5 -> Priority.P2;
            default -> Priority.P3;
        };
        if (occurrences >= 15 && base.ordinal() > 0) {
            return Priority.values()[base.ordinal() - 1];
        }
        return base;
    }

    private Effort deriveEffort(int occurrences, int affectedFiles) {
        if (occurrences <= 3 && affectedFiles <= 2) {
            return Effort.LOW;
        }
        return occurrences > 20 || affectedFiles > 10 ? Effort.HIGH : Effort.MEDIUM;
    }

    private String defaultRationale(FindingView worst, int occurrences) {
        String severity = switch (worst.severityRank()) {
            case 10 -> "critico";
            case 8 -> "alto";
            case 5 -> "medio";
            default -> "bajo";
        };
        String reference = worst.cwe() != null ? " Clasificado como " + worst.cwe() + "." : "";
        return "Riesgo %s con %d ocurrencia(s).%s Dejarlo sin corregir mantiene la exposicion en cada despliegue."
                .formatted(severity, occurrences, reference);
    }

    private String defaultOwner(String category) {
        if (category == null) {
            return "Equipo de desarrollo";
        }
        return switch (category.toUpperCase()) {
            case "SECRET" -> "Seguridad y DevOps";
            case "DEPENDENCY" -> "Mantenimiento de dependencias";
            case "CONFIGURATION" -> "DevOps / Plataforma";
            case "SECURITY" -> "Equipo de desarrollo con revision de seguridad";
            default -> "Equipo de desarrollo";
        };
    }

    /**
     * Recomendaciones que no nacen de una linea de codigo sino del estado global
     * del proyecto. Se derivan de los sub-scores calculados por el analizador.
     */
    private List<Scored> systemicRecommendations(AnalysisView analysis) {
        List<Scored> result = new ArrayList<>();
        Map<String, Double> sub = analysis.subScores() == null ? Map.of() : analysis.subScores();

        if (score(sub, "pruebas") < 50) {
            result.add(systemic(Priority.P1, Effort.HIGH, "Construir una red de pruebas automatizadas",
                    "La proporcion de archivos de prueba frente a codigo de produccion es %.0f%% del objetivo (ratio %.2f)."
                            .formatted(score(sub, "pruebas"), analysis.testFileRatio()),
                    "Empieza cubriendo con pruebas los modulos senalados en las recomendaciones P0 y P1: "
                            + "una prueba por defecto corregido evita que el problema regrese. Fija un umbral "
                            + "de cobertura en el pipeline y hazlo crecer de forma incremental.",
                    "Sin pruebas cada correccion de seguridad es una apuesta: no hay forma de saber si el "
                            + "arreglo funciona ni de evitar que alguien lo revierta sin darse cuenta.",
                    "MAINTAINABILITY", "Equipo de desarrollo", 6.0));
        }

        if (score(sub, "duplicacion") < 70) {
            result.add(systemic(Priority.P2, Effort.MEDIUM, "Eliminar la duplicacion estructural",
                    "El %.1f%% de las lineas de codigo participa en bloques duplicados."
                            .formatted(analysis.duplicationRatio() * 100),
                    "Extrae los bloques repetidos a funciones o modulos compartidos, empezando por los "
                            + "clones con mas copias que aparecen en el detalle del analisis.",
                    "Cada copia es un sitio mas donde hay que aplicar la misma correccion; "
                            + "es la causa habitual de defectos que reaparecen tras haberlos arreglado.",
                    "MAINTAINABILITY", "Equipo de desarrollo", 4.0));
        }

        if (score(sub, "complejidad") < 60) {
            result.add(systemic(Priority.P2, Effort.MEDIUM, "Reducir la complejidad de las funciones criticas",
                    "La complejidad ciclomatica maxima alcanza %d y la media por funcion es %.1f."
                            .formatted(analysis.maxComplexity(), analysis.avgComplexity()),
                    "Refactoriza las funciones senaladas como complejas: extrae ramas a funciones con nombre, "
                            + "usa retorno temprano y sustituye cadenas de condicionales por tablas de decision.",
                    "Las funciones muy ramificadas concentran los defectos y son practicamente imposibles "
                            + "de cubrir con pruebas de forma completa.",
                    "MAINTAINABILITY", "Equipo de desarrollo", 4.5));
        }

        if (score(sub, "documentacion") < 50) {
            result.add(systemic(Priority.P3, Effort.LOW, "Documentar los modulos con mas carga de logica",
                    "Solo el %.1f%% de las lineas son comentarios frente al codigo efectivo."
                            .formatted(analysis.codeLines() == 0 ? 0.0
                                    : analysis.commentLines() * 100.0 / analysis.codeLines()),
                    "Anade una descripcion de proposito en cada modulo publico y documenta las decisiones "
                            + "no obvias (por que, no que). Prioriza los archivos que aparecen en este reporte.",
                    "La falta de contexto multiplica el tiempo de incorporacion de nuevas personas y "
                            + "provoca que se reintroduzcan problemas ya resueltos.",
                    "MAINTAINABILITY", "Equipo de desarrollo", 2.0));
        }

        if (analysis.declaredDependencies() == 0) {
            result.add(systemic(Priority.P3, Effort.LOW, "Declarar y fijar las dependencias del proyecto",
                    "No se encontro ningun manifiesto de dependencias con versiones legibles.",
                    "Anade un manifiesto con versiones fijadas (package.json, pom.xml, requirements.txt) "
                            + "y un archivo de bloqueo para que el build sea reproducible.",
                    "Sin manifiesto no es posible saber que bibliotecas se usan ni si tienen "
                            + "vulnerabilidades conocidas: el analisis de composicion queda ciego.",
                    "DEPENDENCY", "Equipo de desarrollo", 1.5));
        }

        if (analysis.criticalCount() > 0) {
            result.add(systemic(Priority.P0, Effort.LOW, "Bloquear el despliegue hasta cerrar los criticos",
                    "Hay %d hallazgo(s) de gravedad critica sin resolver.".formatted(analysis.criticalCount()),
                    "Anade una puerta de calidad en el pipeline que falle el build cuando exista al menos "
                            + "un hallazgo critico, y trata los actuales como incidencia antes de la proxima release.",
                    "Un hallazgo critico suele permitir ejecucion de codigo, acceso a datos ajenos o "
                            + "uso de credenciales filtradas: el coste de explotarlo es bajo y el impacto total.",
                    "SECURITY", "Seguridad y DevOps", 12.0, analysis.criticalCount()));
        }

        return result;
    }

    private Scored systemic(Priority priority, Effort effort, String title, String problem,
                            String action, String rationale, String category, String owner, double weight) {
        return systemic(priority, effort, title, problem, action, rationale, category, owner, weight, 0);
    }

    /**
     * Variante con conteo: algunas recomendaciones sistemicas si tienen un numero
     * de hallazgos asociado (por ejemplo la puerta de calidad sobre los criticos).
     */
    private Scored systemic(Priority priority, Effort effort, String title, String problem,
                            String action, String rationale, String category, String owner,
                            double weight, int findingsCount) {
        Recommendation recommendation = new Recommendation(priority, effort, title, problem, action,
                rationale, category);
        recommendation.setOwnerRole(owner);
        recommendation.setFindingsCount(findingsCount);
        return new Scored(recommendation, priority, weight);
    }

    private double score(Map<String, Double> subScores, String key) {
        return subScores.getOrDefault(key, 100.0);
    }

    private Priority parsePriority(String value) {
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Priority.P2;
        }
    }

    private Effort parseEffort(String value) {
        try {
            return Effort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Effort.MEDIUM;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    /** Recomendacion con los datos necesarios para ordenarla. */
    private record Scored(Recommendation recommendation, Priority priority, double weight) {
    }
}
