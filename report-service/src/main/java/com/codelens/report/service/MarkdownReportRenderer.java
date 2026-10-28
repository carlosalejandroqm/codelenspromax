package com.codelens.report.service;

import com.codelens.report.client.AnalysisView;
import com.codelens.report.client.FindingView;
import com.codelens.report.model.Report;
import com.codelens.report.model.Recommendation;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renderiza el reporte completo en Markdown.
 *
 * Se elige Markdown como formato canonico porque es legible tal cual, se
 * convierte a HTML o PDF sin dependencias y se puede pegar en un pull request o
 * en la wiki del equipo.
 */
@Component
public class MarkdownReportRenderer {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final int MAX_FINDINGS_LISTED = 60;

    public String render(Report report, AnalysisView analysis, List<FindingView> findings) {
        StringBuilder md = new StringBuilder();

        header(md, report, analysis);
        scorecard(md, analysis);
        summary(md, report);
        remediationPlan(md, report);
        metrics(md, analysis);
        languages(md, analysis);
        findingsTable(md, findings);
        methodology(md, analysis);

        return md.toString();
    }

    private void header(StringBuilder md, Report report, AnalysisView analysis) {
        md.append("# Reporte de analisis de codigo: ").append(report.getProjectName()).append("\n\n");
        md.append("**CodeLens Pro Max** · generado el ")
                .append(TIMESTAMP.format(report.getGeneratedAt())).append("\n\n");
        md.append("| | |\n|---|---|\n");
        md.append("| Veredicto | **").append(readableVerdict(report.getVerdict())).append("** |\n");
        md.append("| Calificacion | **").append(report.getGrade()).append("** (")
                .append(format(report.getOverallScore())).append("/100) |\n");
        md.append("| Nivel de riesgo | ").append(nullSafe(report.getRiskLevel())).append(" |\n");
        md.append("| Archivos analizados | ").append(analysis.filesAnalyzed()).append(" |\n");
        md.append("| Lineas de codigo | ").append(String.format("%,d", analysis.codeLines())).append(" |\n");
        md.append("| Duracion del analisis | ").append(analysis.durationMillis()).append(" ms |\n");
        md.append("| Id de analisis | `").append(report.getAnalysisId()).append("` |\n\n");
    }

    private void scorecard(StringBuilder md, AnalysisView analysis) {
        md.append("## Puntuaciones\n\n");
        md.append("| Dimension | Puntaje | Barra |\n|---|---:|---|\n");
        md.append(scoreRow("Seguridad", analysis.securityScore()));
        md.append(scoreRow("Salud del codigo", analysis.healthScore()));
        md.append(scoreRow("Global", analysis.overallScore()));
        md.append("\n");

        if (analysis.subScores() != null && !analysis.subScores().isEmpty()) {
            md.append("### Desglose de la salud del codigo\n\n");
            md.append("| Factor | Puntaje | Barra |\n|---|---:|---|\n");
            analysis.subScores().entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(entry -> md.append(scoreRow(readableFactor(entry.getKey()), entry.getValue())));
            md.append("\n");
        }
    }

    private String scoreRow(String label, double score) {
        return "| %s | %s | %s |\n".formatted(label, format(score), bar(score));
    }

    /** Barra de progreso en texto: legible en Markdown sin necesitar graficos. */
    private String bar(double score) {
        int filled = (int) Math.round(Math.max(0, Math.min(100, score)) / 10.0);
        return "`" + "#".repeat(filled) + ".".repeat(10 - filled) + "`";
    }

    private void summary(StringBuilder md, Report report) {
        md.append("## Resumen ejecutivo\n\n").append(report.getExecutiveSummary()).append("\n\n");

        if (!report.getStrengths().isEmpty()) {
            md.append("### Puntos fuertes\n\n");
            report.getStrengths().forEach(s -> md.append("- ").append(s).append("\n"));
            md.append("\n");
        }
        if (!report.getKeyRisks().isEmpty()) {
            md.append("### Riesgos principales\n\n");
            report.getKeyRisks().forEach(r -> md.append("- ").append(r).append("\n"));
            md.append("\n");
        }
    }

    private void remediationPlan(StringBuilder md, Report report) {
        md.append("## Plan de remediacion\n\n");
        if (report.getRecommendations().isEmpty()) {
            md.append("No hay acciones pendientes con el catalogo de reglas activo.\n\n");
            return;
        }

        md.append("| # | Prioridad | Cuando | Accion | Esfuerzo | Hallazgos |\n|---:|---|---|---|---|---:|\n");
        for (Recommendation r : report.getRecommendations()) {
            md.append("| %d | %s | %s | %s | %s | %d |\n".formatted(
                    r.getRank(), r.getPriority().name(), r.getPriority().getWindow(),
                    escape(r.getTitle()), r.getEffort().getLabel(), r.getFindingsCount()));
        }
        md.append("\n");

        for (Recommendation r : report.getRecommendations()) {
            md.append("### ").append(r.getRank()).append(". ").append(r.getTitle())
                    .append(" · ").append(r.getPriority().name()).append("\n\n");
            md.append("- **Prioridad**: ").append(r.getPriority().getLabel())
                    .append(" (").append(r.getPriority().getWindow()).append(")\n");
            md.append("- **Esfuerzo estimado**: ").append(r.getEffort().getLabel())
                    .append(" (").append(r.getEffort().getEstimate()).append(")\n");
            if (r.getOwnerRole() != null) {
                md.append("- **Responsable sugerido**: ").append(r.getOwnerRole()).append("\n");
            }
            if (r.getCwe() != null || r.getOwasp() != null) {
                md.append("- **Clasificacion**: ")
                        .append(nullSafe(r.getCwe()))
                        .append(r.getCwe() != null && r.getOwasp() != null ? " · " : "")
                        .append(nullSafe(r.getOwasp())).append("\n");
            }
            md.append("\n**Problema.** ").append(r.getProblem()).append("\n\n");
            md.append("**Que hacer.** ").append(r.getAction()).append("\n\n");
            md.append("**Por que importa.** ").append(r.getRationale()).append("\n\n");

            if (!r.getExamples().isEmpty()) {
                md.append("**Donde empezar.**\n\n");
                r.getExamples().forEach(e -> md.append("- `").append(e).append("`\n"));
                md.append("\n");
            }
            if (!r.getReferences().isEmpty()) {
                md.append("**Referencias.**\n\n");
                r.getReferences().forEach(ref -> md.append("- <").append(ref).append(">\n"));
                md.append("\n");
            }
        }
    }

    private void metrics(StringBuilder md, AnalysisView analysis) {
        md.append("## Metricas de salud\n\n");
        md.append("| Metrica | Valor |\n|---|---:|\n");
        md.append("| Lineas totales | ").append(String.format("%,d", analysis.totalLines())).append(" |\n");
        md.append("| Lineas de codigo | ").append(String.format("%,d", analysis.codeLines())).append(" |\n");
        md.append("| Lineas de comentario | ").append(String.format("%,d", analysis.commentLines())).append(" |\n");
        md.append("| Lineas en blanco | ").append(String.format("%,d", analysis.blankLines())).append(" |\n");
        md.append("| Ratio de comentarios | ").append(percent(analysis.codeLines() == 0 ? 0
                : (double) analysis.commentLines() / analysis.codeLines())).append(" |\n");
        md.append("| Duplicacion de codigo | ").append(percent(analysis.duplicationRatio())).append(" |\n");
        md.append("| Complejidad media por funcion | ").append(format(analysis.avgComplexity())).append(" |\n");
        md.append("| Complejidad maxima | ").append(analysis.maxComplexity()).append(" |\n");
        md.append("| Ratio archivos de prueba | ").append(format(analysis.testFileRatio())).append(" |\n");
        md.append("| Marcas de deuda tecnica | ").append(analysis.todoCount()).append(" |\n");
        md.append("| Dependencias declaradas | ").append(analysis.declaredDependencies()).append(" |\n\n");
    }

    private void languages(StringBuilder md, AnalysisView analysis) {
        if (analysis.languages() == null || analysis.languages().isEmpty()) {
            return;
        }
        md.append("## Composicion por lenguaje\n\n");
        md.append("| Lenguaje | Archivos | Lineas de codigo | % |\n|---|---:|---:|---:|\n");
        analysis.languages().forEach(l -> md.append("| %s | %d | %,d | %.1f%% |\n"
                .formatted(l.language(), l.files(), l.codeLines(), l.percentage())));
        md.append("\n");
    }

    private void findingsTable(StringBuilder md, List<FindingView> findings) {
        md.append("## Detalle de hallazgos\n\n");
        if (findings.isEmpty()) {
            md.append("Sin hallazgos.\n\n");
            return;
        }

        Map<String, List<FindingView>> byCategory = findings.stream()
                .collect(Collectors.groupingBy(
                        f -> f.category() == null ? "OTROS" : f.category(),
                        LinkedHashMap::new, Collectors.toList()));

        int listed = 0;
        for (var entry : byCategory.entrySet()) {
            md.append("### ").append(readableCategory(entry.getKey()))
                    .append(" (").append(entry.getValue().size()).append(")\n\n");
            md.append("| Gravedad | Hallazgo | Ubicacion | Confianza | CWE |\n|---|---|---|---:|---|\n");

            List<FindingView> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(FindingView::severityRank).reversed()
                            .thenComparing(Comparator.comparingDouble(FindingView::confidence).reversed()))
                    .toList();

            for (FindingView f : sorted) {
                if (listed++ >= MAX_FINDINGS_LISTED) {
                    break;
                }
                md.append("| %s | %s | `%s` | %.0f%% | %s |\n".formatted(
                        f.severity(), escape(f.title()), f.location(),
                        f.confidence() * 100, nullSafe(f.cwe())));
            }
            md.append("\n");
        }

        if (findings.size() > MAX_FINDINGS_LISTED) {
            md.append("> Se listan los %d hallazgos mas relevantes de %d. Consulta el detalle completo en la API.\n\n"
                    .formatted(MAX_FINDINGS_LISTED, findings.size()));
        }
    }

    private void methodology(StringBuilder md, AnalysisView analysis) {
        md.append("## Como se obtuvo este reporte\n\n");
        md.append("1. **Obtencion del codigo** (source-service): se materializo un snapshot inmutable del "
                + "repositorio o del zip y se inventariaron los archivos de texto.\n");
        md.append("2. **Analisis** (analysis-service): un motor agnostico del lenguaje normalizo cada archivo "
                + "separando codigo, estructura y comentarios, y aplico el catalogo de reglas declarativas, "
                + "la deteccion de secretos por entropia, las metricas de mantenibilidad, la deteccion de "
                + "clones y el cruce de dependencias con la base de avisos.\n");
        md.append("3. **Reporte** (report-service): los hallazgos se agruparon por causa, se priorizaron por "
                + "gravedad y volumen y se enriquecieron con playbooks de remediacion.\n\n");
        md.append("> El analisis es estatico: senala patrones de riesgo sobre el texto del codigo. "
                + "Un hallazgo con confianza baja puede ser un falso positivo y conviene revisarlo; "
                + "la ausencia de hallazgos no demuestra ausencia de vulnerabilidades.\n");
    }

    private String readableVerdict(String verdict) {
        return switch (verdict) {
            case VerdictResolver.REJECTED -> "Rechazado";
            case VerdictResolver.APPROVED_WITH_RESERVATIONS -> "Aprobado con reservas";
            default -> "Aprobado";
        };
    }

    private String readableCategory(String category) {
        return switch (category.toUpperCase()) {
            case "SECURITY" -> "Seguridad";
            case "SECRET" -> "Secretos y credenciales";
            case "DEPENDENCY" -> "Dependencias";
            case "CONFIGURATION" -> "Configuracion";
            case "MAINTAINABILITY" -> "Mantenibilidad";
            case "TECHNICAL_DEBT" -> "Deuda tecnica";
            default -> category;
        };
    }

    private String readableFactor(String factor) {
        return switch (factor) {
            case "documentacion" -> "Documentacion";
            case "complejidad" -> "Complejidad";
            case "duplicacion" -> "Duplicacion";
            case "tamano_archivos" -> "Tamano de archivos";
            case "pruebas" -> "Pruebas";
            case "deuda_declarada" -> "Deuda declarada";
            case "anidamiento" -> "Anidamiento";
            default -> factor;
        };
    }

    private String format(double value) {
        return String.format("%.1f", value);
    }

    private String percent(double ratio) {
        return String.format("%.1f%%", ratio * 100);
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }

    /** Evita que un titulo con '|' rompa la tabla Markdown. */
    private String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }
}
