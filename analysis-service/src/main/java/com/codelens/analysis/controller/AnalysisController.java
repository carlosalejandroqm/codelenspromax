package com.codelens.analysis.controller;

import com.codelens.analysis.dto.*;
import com.codelens.analysis.model.Analysis;
import com.codelens.analysis.service.AnalysisOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API del microservicio de analisis. */
@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    /** Identidad que agrega el gateway tras validar el token. */
    private static final String USER_HEADER = "X-User-Id";

    private final AnalysisOrchestrator orchestrator;

    public AnalysisController(AnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /** Lanza el analisis de un snapshot ya obtenido por el source-service. */
    @PostMapping
    public ResponseEntity<AnalysisDto> analyze(@Valid @RequestBody AnalysisRequest request,
                                               @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        Analysis analysis = orchestrator.analyze(request.sourceId(), ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(AnalysisDto.from(analysis));
    }

    @GetMapping
    public List<AnalysisDto> list(@RequestParam(required = false) String sourceId,
                                  @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        List<Analysis> analyses = sourceId == null || sourceId.isBlank()
                ? orchestrator.list(ownerId)
                : orchestrator.listBySource(sourceId);
        return analyses.stream().map(AnalysisDto::from).toList();
    }

    @GetMapping("/{id}")
    public AnalysisDto get(@PathVariable String id) {
        return AnalysisDto.from(orchestrator.get(id));
    }

    /** Analisis con todos sus hallazgos; es lo que consume el report-service. */
    @GetMapping("/{id}/detail")
    public AnalysisDetailDto detail(@PathVariable String id) {
        Analysis analysis = orchestrator.get(id);
        List<FindingDto> findings = orchestrator.findings(id).stream().map(FindingDto::from).toList();
        return new AnalysisDetailDto(AnalysisDto.from(analysis), findings);
    }

    @GetMapping("/{id}/findings")
    public List<FindingDto> findings(@PathVariable String id) {
        return orchestrator.findings(id).stream().map(FindingDto::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        orchestrator.delete(id);
        return ResponseEntity.noContent().build();
    }
}
