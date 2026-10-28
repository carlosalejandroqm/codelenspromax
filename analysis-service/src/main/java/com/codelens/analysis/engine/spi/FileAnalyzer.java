package com.codelens.analysis.engine.spi;

import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.AnalysisUnit;

/**
 * Analizador que examina un archivo de forma aislada.
 *
 * Es el punto de extension del motor: cualquier bean que implemente esta
 * interfaz entra automaticamente en el pipeline (Spring lo inyecta en la lista),
 * sin registrar nada a mano y sin que el motor conozca su lenguaje objetivo.
 */
public interface FileAnalyzer {

    /** Identificador estable, aparece en los logs y en el detalle del analisis. */
    String id();

    /** Filtro opcional; por defecto un analizador acepta cualquier archivo. */
    default boolean supports(AnalysisUnit unit) {
        return true;
    }

    void analyze(AnalysisUnit unit, AnalysisCollector collector);
}
