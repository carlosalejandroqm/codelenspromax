package com.codelens.analysis.engine.spi;

import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.ProjectContext;

/**
 * Analizador que necesita ver el proyecto completo (duplicacion entre archivos,
 * dependencias declaradas en manifiestos, etc.). Se ejecuta despues de todos
 * los {@link FileAnalyzer}.
 */
public interface ProjectAnalyzer {

    String id();

    void analyze(ProjectContext context, AnalysisCollector collector);
}
