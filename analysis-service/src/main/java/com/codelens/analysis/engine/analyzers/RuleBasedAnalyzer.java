package com.codelens.analysis.engine.analyzers;

import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.CompiledRule;
import com.codelens.analysis.engine.RuleCatalog;
import com.codelens.analysis.engine.model.AnalysisUnit;
import com.codelens.analysis.engine.model.RawFinding;
import com.codelens.analysis.engine.spi.FileAnalyzer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Motor de reglas declarativas: por cada archivo evalua las reglas del catalogo
 * que aplican a su lenguaje y ruta.
 *
 * No contiene ninguna regla escrita en Java. Todo el conocimiento de
 * vulnerabilidades vive en JSON, por lo que anadir cobertura para un lenguaje
 * nuevo no requiere tocar esta clase.
 */
@Component
public class RuleBasedAnalyzer implements FileAnalyzer {

    private final RuleCatalog catalog;

    public RuleBasedAnalyzer(RuleCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String id() {
        return "rule-engine";
    }

    @Override
    public void analyze(AnalysisUnit unit, AnalysisCollector collector) {
        List<CompiledRule> rules = catalog.rulesFor(unit.path(), unit.profile());
        if (rules.isEmpty()) {
            return;
        }

        for (CompiledRule rule : rules) {
            // Las reglas de deuda tecnica miran comentarios; las de seguridad, codigo.
            List<String> haystack = rule.scansComments() ? unit.commentLines() : unit.codeLines();
            for (int i = 0; i < haystack.size(); i++) {
                String line = haystack.get(i);
                if (line.isBlank() || !rule.matches(line)) {
                    continue;
                }
                int lineNumber = i + 1;
                var definition = rule.definition();
                collector.add(RawFinding.builder(definition.id())
                        .title(definition.name())
                        .description(definition.description())
                        .category(definition.category())
                        .severity(definition.severity())
                        .confidence(adjustConfidence(definition.confidence(), unit))
                        .cwe(definition.cwe())
                        .owasp(definition.owasp())
                        .filePath(unit.path())
                        .lineNumber(lineNumber)
                        .evidence(unit.evidenceAt(lineNumber))
                        .language(unit.languageName())
                        .remediation(definition.remediation())
                        .build());
            }
        }
    }

    /**
     * Un hallazgo en codigo de pruebas es menos relevante: se baja la confianza
     * en lugar de descartarlo, porque un secreto real en un test sigue siendo un
     * secreto filtrado.
     */
    private double adjustConfidence(double base, AnalysisUnit unit) {
        return unit.testFile() ? Math.max(0.2, base - 0.25) : base;
    }
}
