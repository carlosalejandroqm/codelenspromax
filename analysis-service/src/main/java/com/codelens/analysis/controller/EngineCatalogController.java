package com.codelens.analysis.controller;

import com.codelens.analysis.dto.EngineInfoDto;
import com.codelens.analysis.engine.AdvisoryDatabase;
import com.codelens.analysis.engine.AnalysisEngine;
import com.codelens.analysis.engine.LanguageRegistry;
import com.codelens.analysis.engine.RuleCatalog;
import com.codelens.analysis.engine.model.LanguageProfile;
import com.codelens.analysis.engine.model.RuleDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Catalogo del motor. Expone reglas y perfiles de lenguaje para que el frontend
 * pueda mostrar la cobertura real del analizador en lugar de una lista fija.
 */
@RestController
@RequestMapping("/api/v1")
public class EngineCatalogController {

    private final RuleCatalog ruleCatalog;
    private final LanguageRegistry languageRegistry;
    private final AdvisoryDatabase advisoryDatabase;
    private final AnalysisEngine engine;

    public EngineCatalogController(RuleCatalog ruleCatalog, LanguageRegistry languageRegistry,
                                   AdvisoryDatabase advisoryDatabase, AnalysisEngine engine) {
        this.ruleCatalog = ruleCatalog;
        this.languageRegistry = languageRegistry;
        this.advisoryDatabase = advisoryDatabase;
        this.engine = engine;
    }

    @GetMapping("/rules")
    public List<RuleDefinition> rules() {
        return ruleCatalog.definitions();
    }

    @GetMapping("/rules/info")
    public EngineInfoDto info() {
        return new EngineInfoDto(
                engine.activeAnalyzers(),
                languageRegistry.all().size(),
                ruleCatalog.size(),
                advisoryDatabase.advisoryCount(),
                advisoryDatabase.manifestFormats().size(),
                ruleCatalog.loadErrors());
    }

    /** Lenguajes reconocidos y sus extensiones, incluido el perfil generico. */
    @GetMapping("/languages")
    public List<Map<String, Object>> languages() {
        return languageRegistry.all().stream()
                .map(this::describe)
                .toList();
    }

    private Map<String, Object> describe(LanguageProfile profile) {
        return Map.of(
                "id", profile.id(),
                "name", profile.name(),
                "extensions", profile.extensions(),
                "blockStyle", profile.blockStyle().name(),
                "branchKeywords", profile.branchKeywords().size(),
                "detectsFunctions", !profile.functionPatterns().isEmpty(),
                "generic", profile.generic());
    }
}
