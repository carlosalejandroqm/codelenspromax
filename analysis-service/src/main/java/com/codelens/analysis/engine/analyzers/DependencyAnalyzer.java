package com.codelens.analysis.engine.analyzers;

import com.codelens.analysis.engine.AdvisoryDatabase;
import com.codelens.analysis.engine.AnalysisCollector;
import com.codelens.analysis.engine.model.*;
import com.codelens.analysis.engine.spi.ProjectAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Analisis de composicion (SCA): localiza los manifiestos de dependencias del
 * proyecto, extrae los pares paquete/version con los patrones declarativos de
 * {@code manifests.json} y los cruza con la base de avisos offline.
 *
 * El analizador no conoce npm ni Maven ni pip: recibe el formato como
 * configuracion, asi que anadir un gestor de paquetes no cambia este codigo.
 */
@Component
public class DependencyAnalyzer implements ProjectAnalyzer {

    /** Gauge con el numero de dependencias declaradas que se pudieron leer. */
    public static final String GAUGE_DEPENDENCIES = "dependencies.declared";

    private static final Logger log = LoggerFactory.getLogger(DependencyAnalyzer.class);

    private final AdvisoryDatabase advisories;

    public DependencyAnalyzer(AdvisoryDatabase advisories) {
        this.advisories = advisories;
    }

    @Override
    public String id() {
        return "dependency-sca";
    }

    @Override
    public void analyze(ProjectContext context, AnalysisCollector collector) {
        Set<String> seen = new HashSet<>();
        int declared = 0;

        for (AnalysisUnit unit : context.units()) {
            List<AdvisoryDatabase.CompiledManifestFormat> formats = advisories.formatsFor(unit.path());
            if (formats.isEmpty()) {
                continue;
            }

            String content = ProjectContext.contentOf(unit);
            for (var format : formats) {
                Matcher matcher = format.entryPattern().matcher(content);
                while (matcher.find()) {
                    String name = group(matcher, "name");
                    String version = group(matcher, "version");
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    declared++;
                    String ecosystem = format.format().ecosystem();
                    if (!seen.add(ecosystem + "::" + name + "::" + version)) {
                        continue;
                    }
                    reportAdvisories(unit, content, ecosystem, name, version, collector);
                }
            }
        }

        collector.putGauge(GAUGE_DEPENDENCIES, declared);
        log.debug("Dependencias declaradas detectadas: {}", declared);
    }

    private void reportAdvisories(AnalysisUnit unit, String content, String ecosystem,
                                  String name, String version, AnalysisCollector collector) {
        for (Advisory advisory : advisories.findAffecting(ecosystem, name, version)) {
            int line = lineOf(content, name);
            collector.add(RawFinding.builder("DEP-" + advisory.id())
                    .title("Dependencia vulnerable: %s %s".formatted(name, version))
                    .description(("%s %s. %s Aviso %s%s.")
                            .formatted(name, version,
                                    advisory.description() == null ? advisory.title() : advisory.description(),
                                    advisory.id(),
                                    advisory.cve() == null ? "" : " (" + advisory.cve() + ")"))
                    .category(FindingCategory.DEPENDENCY)
                    .severity(advisory.severity())
                    .confidence(0.9)
                    .cwe(advisory.cwe())
                    .owasp("A06:2021-Vulnerable and Outdated Components")
                    .filePath(unit.path())
                    .lineNumber(line)
                    .evidence(unit.evidenceAt(line))
                    .language(unit.languageName())
                    .remediation(advisory.remediation() != null ? advisory.remediation()
                            : "Actualiza %s a la version %s o superior.".formatted(name,
                                    advisory.fixedVersion() == null ? "corregida" : advisory.fixedVersion()))
                    .build());
        }
    }

    /** Localiza la linea del manifiesto donde se declara el paquete. */
    private int lineOf(String content, String packageName) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(packageName)) {
                return i + 1;
            }
        }
        return 1;
    }

    /** Lee un grupo nombrado tolerando que el patron no lo defina. */
    private String group(Matcher matcher, String name) {
        try {
            return matcher.group(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
