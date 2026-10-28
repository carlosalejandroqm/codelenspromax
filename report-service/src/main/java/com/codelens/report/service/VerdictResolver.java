package com.codelens.report.service;

import com.codelens.report.client.AnalysisView;
import org.springframework.stereotype.Component;

/**
 * Decide el veredicto del reporte. Se aisla en su propia clase porque es la
 * regla de negocio que un equipo querra ajustar a su politica de calidad.
 */
@Component
public class VerdictResolver {

    public static final String APPROVED = "APROBADO";
    public static final String APPROVED_WITH_RESERVATIONS = "APROBADO_CON_RESERVAS";
    public static final String REJECTED = "RECHAZADO";

    /**
     * Un critico o dos o mas altos bloquean; por debajo de eso la nota global
     * decide entre aprobado y aprobado con reservas.
     */
    public String resolve(AnalysisView analysis) {
        if (analysis.criticalCount() > 0 || analysis.highCount() >= 2) {
            return REJECTED;
        }
        if (analysis.highCount() > 0 || analysis.overallScore() < 70) {
            return APPROVED_WITH_RESERVATIONS;
        }
        return APPROVED;
    }

    public String explain(String verdict) {
        return switch (verdict) {
            case REJECTED -> "No se recomienda desplegar: hay hallazgos de gravedad critica o varios de "
                    + "gravedad alta que deben cerrarse primero.";
            case APPROVED_WITH_RESERVATIONS -> "Se puede desplegar asumiendo riesgo conocido, pero hay que "
                    + "planificar la remediacion en el sprint en curso.";
            default -> "No se detectan bloqueantes; el codigo esta en condiciones de seguir a produccion.";
        };
    }
}
