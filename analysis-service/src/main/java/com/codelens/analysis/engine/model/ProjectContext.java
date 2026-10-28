package com.codelens.analysis.engine.model;

import java.util.List;

/**
 * Vista de todo el proyecto para los analizadores que necesitan mirar mas de un
 * archivo a la vez (duplicacion entre archivos, dependencias declaradas,
 * cobertura de pruebas).
 *
 * @param projectName nombre del proyecto analizado
 * @param units       archivos ya normalizados, en orden de ruta
 */
public record ProjectContext(
        String projectName,
        List<AnalysisUnit> units
) {

    /** Reconstruye el contenido original de un archivo del proyecto. */
    public static String contentOf(AnalysisUnit unit) {
        return String.join("\n", unit.rawLines());
    }
}
