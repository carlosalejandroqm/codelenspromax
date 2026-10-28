package com.codelens.report.service;

import com.codelens.report.client.AnalysisView;
import com.codelens.report.client.FindingView;
import com.codelens.report.model.Priority;
import com.codelens.report.model.Recommendation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redacta el resumen ejecutivo, los puntos fuertes y los riesgos clave.
 *
 * El texto se compone a partir de los datos del analisis, no de plantillas
 * fijas: si el proyecto no tiene secretos filtrados, el resumen no menciona
 * secretos. La idea es que alguien que no va a leer la lista de hallazgos
 * entienda en un parrafo en que estado esta el codigo.
 */
@Component
public class ExecutiveSummaryWriter {

    private final VerdictResolver verdictResolver;

    public ExecutiveSummaryWriter(VerdictResolver verdictResolver) {
        this.verdictResolver = verdictResolver;
    }

    public String write(AnalysisView analysis, List<FindingView> findings,
                        List<Recommendation> recommendations, String verdict) {
        StringBuilder summary = new StringBuilder();

        summary.append("Se analizaron %d archivos (%,d lineas de codigo) del proyecto '%s'"
                .formatted(analysis.filesAnalyzed(), analysis.codeLines(), analysis.projectName()));
        if (analysis.languages() != null && !analysis.languages().isEmpty()) {
            var main = analysis.languages().get(0);
            summary.append(", predominantemente %s (%.1f%% del codigo)"
                    .formatted(main.language(), main.percentage()));
        }
        summary.append(". ");

        summary.append("La calificacion global es %s con %.1f/100 puntos, resultado de %.1f en seguridad y %.1f en salud del codigo. "
                .formatted(analysis.grade(), analysis.overallScore(),
                        analysis.securityScore(), analysis.healthScore()));

        if (analysis.totalFindings() == 0) {
            summary.append("No se detectaron hallazgos con las reglas activas. ");
        } else {
            summary.append("Se detectaron %d hallazgos: %d criticos, %d altos, %d medios, %d bajos y %d informativos. "
                    .formatted(analysis.totalFindings(), analysis.criticalCount(), analysis.highCount(),
                            analysis.mediumCount(), analysis.lowCount(), analysis.infoCount()));
        }

        if (analysis.secretFindings() > 0) {
            summary.append("Hay %d posible(s) credencial(es) embebida(s) en el repositorio, lo que exige rotarlas ademas de retirarlas del codigo. "
                    .formatted(analysis.secretFindings()));
        }
        if (analysis.dependencyFindings() > 0) {
            summary.append("Se identificaron %d problema(s) en dependencias o configuracion de despliegue. "
                    .formatted(analysis.dependencyFindings()));
        }

        long immediate = recommendations.stream().filter(r -> r.getPriority() == Priority.P0).count();
        if (immediate > 0) {
            summary.append("El plan de remediacion incluye %d accion(es) de prioridad inmediata. ".formatted(immediate));
        }

        summary.append(verdictResolver.explain(verdict));
        return summary.toString();
    }

    /** Lo que el proyecto hace bien; sin esto el reporte se ignora. */
    public List<String> strengths(AnalysisView analysis, List<FindingView> findings) {
        List<String> strengths = new ArrayList<>();
        Map<String, Double> sub = analysis.subScores() == null ? Map.of() : analysis.subScores();

        if (analysis.criticalCount() == 0) {
            strengths.add("Sin hallazgos de gravedad critica.");
        }
        if (analysis.secretFindings() == 0) {
            strengths.add("No se detectaron credenciales embebidas en el codigo.");
        }
        if (analysis.dependencyFindings() == 0 && analysis.declaredDependencies() > 0) {
            strengths.add("Las %d dependencias declaradas no coinciden con ningun aviso conocido del catalogo."
                    .formatted(analysis.declaredDependencies()));
        }
        if (sub.getOrDefault("pruebas", 0.0) >= 70) {
            strengths.add("Proporcion saludable de archivos de prueba (ratio %.2f)."
                    .formatted(analysis.testFileRatio()));
        }
        if (sub.getOrDefault("duplicacion", 0.0) >= 85) {
            strengths.add("Duplicacion de codigo baja (%.1f%% de las lineas)."
                    .formatted(analysis.duplicationRatio() * 100));
        }
        if (sub.getOrDefault("documentacion", 0.0) >= 80) {
            strengths.add("El codigo esta razonablemente documentado.");
        }
        if (sub.getOrDefault("complejidad", 0.0) >= 80) {
            strengths.add("Complejidad por funcion contenida (media %.1f).".formatted(analysis.avgComplexity()));
        }
        if (strengths.isEmpty()) {
            strengths.add("El analisis se completo sobre todo el arbol de codigo, "
                    + "lo que da una linea base medible para las proximas iteraciones.");
        }
        return strengths;
    }

    /** Los riesgos que hay que contar primero, ya redactados. */
    public List<String> keyRisks(AnalysisView analysis, List<FindingView> findings) {
        List<String> risks = new ArrayList<>();

        findings.stream()
                .filter(f -> f.severityRank() >= 8 && f.confidence() >= 0.6)
                .limit(5)
                .forEach(f -> risks.add("%s en %s%s"
                        .formatted(f.title(), f.location(),
                                f.cwe() == null ? "" : " (" + f.cwe() + ")")));

        if (risks.isEmpty() && analysis.mediumCount() > 0) {
            risks.add("Sin bloqueantes, pero %d hallazgo(s) de gravedad media acumulan riesgo si no se atienden."
                    .formatted(analysis.mediumCount()));
        }
        if (analysis.subScores() != null && analysis.subScores().getOrDefault("pruebas", 100.0) < 40) {
            risks.add("Cobertura de pruebas muy baja: cualquier correccion se aplica sin red de seguridad.");
        }
        if (risks.isEmpty()) {
            risks.add("No se identificaron riesgos relevantes con el catalogo de reglas activo.");
        }
        return risks;
    }
}
