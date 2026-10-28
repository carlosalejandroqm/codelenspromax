package com.codelens.analysis.engine;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.model.LanguageProfile;
import com.codelens.analysis.engine.model.RuleDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * Catalogo de reglas declarativas. Carga todos los {@code rules/*.json} del
 * classpath y, si se configura, los de un directorio externo, que puede
 * sobrescribir reglas internas reutilizando el mismo id.
 */
@Component
public class RuleCatalog {

    private static final Logger log = LoggerFactory.getLogger(RuleCatalog.class);
    private static final String BUILTIN_PATTERN = "classpath*:rules/*.json";

    private final ObjectMapper objectMapper;
    private final AnalysisProperties properties;

    private final Map<String, CompiledRule> rulesById = new LinkedHashMap<>();
    private final List<String> loadErrors = new ArrayList<>();

    public RuleCatalog(ObjectMapper objectMapper, AnalysisProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        List<RuleDefinition> definitions = new ArrayList<>(readBuiltin());
        definitions.addAll(readExternal());

        for (RuleDefinition definition : definitions) {
            if (definition.id() == null || definition.id().isBlank()) {
                loadErrors.add("Regla sin id ignorada");
                continue;
            }
            if (!definition.enabled()) {
                continue;
            }
            if (definition.pattern() == null || definition.pattern().isBlank()) {
                loadErrors.add(definition.id() + ": sin patron");
                continue;
            }
            try {
                rulesById.put(definition.id(), CompiledRule.of(definition));
            } catch (PatternSyntaxException e) {
                loadErrors.add(definition.id() + ": regex invalido - " + e.getDescription());
                log.warn("Regla {} descartada por regex invalido: {}", definition.id(), e.getDescription());
            }
        }

        log.info("Reglas activas: {} ({} descartadas)", rulesById.size(), loadErrors.size());
    }

    private List<RuleDefinition> readBuiltin() {
        List<RuleDefinition> all = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(BUILTIN_PATTERN);
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    all.addAll(objectMapper.readerForListOf(RuleDefinition.class).readValue(in));
                } catch (IOException e) {
                    loadErrors.add(resource.getFilename() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("No se pudieron localizar las reglas internas", e);
        }
        return all;
    }

    private List<RuleDefinition> readExternal() {
        String dir = properties.getExternalRulesDirectory();
        if (dir == null || dir.isBlank() || !Files.isDirectory(Path.of(dir))) {
            return List.of();
        }
        List<RuleDefinition> all = new ArrayList<>();
        try (Stream<Path> files = Files.list(Path.of(dir))) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("-rules.json")).toList()) {
                try (InputStream in = Files.newInputStream(file)) {
                    all.addAll(objectMapper.readerForListOf(RuleDefinition.class).readValue(in));
                    log.info("Reglas externas cargadas desde {}", file);
                } catch (IOException e) {
                    loadErrors.add(file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo listar el directorio de reglas externas {}: {}", dir, e.getMessage());
        }
        return all;
    }

    /** Reglas aplicables a un archivo concreto, ya filtradas por lenguaje y ruta. */
    public List<CompiledRule> rulesFor(String path, LanguageProfile profile) {
        return rulesById.values().stream()
                .filter(rule -> rule.appliesTo(path, profile))
                .toList();
    }

    public List<RuleDefinition> definitions() {
        return rulesById.values().stream().map(CompiledRule::definition).toList();
    }

    public java.util.Optional<RuleDefinition> definition(String ruleId) {
        CompiledRule rule = rulesById.get(ruleId);
        return java.util.Optional.ofNullable(rule).map(CompiledRule::definition);
    }

    public List<String> loadErrors() {
        return Collections.unmodifiableList(loadErrors);
    }

    public int size() {
        return rulesById.size();
    }
}
