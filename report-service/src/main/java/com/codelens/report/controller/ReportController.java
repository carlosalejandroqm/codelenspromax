package com.codelens.report.controller;

import com.codelens.report.dto.ReportDto;
import com.codelens.report.dto.ReportRequest;
import com.codelens.report.dto.ReportSummaryDto;
import com.codelens.report.model.Report;
import com.codelens.report.service.HtmlReportRenderer;
import com.codelens.report.service.ReportGenerator;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API del microservicio de reportes y recomendaciones. */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    /** Identidad que agrega el gateway tras validar el token. */
    private static final String USER_HEADER = "X-User-Id";

    private final ReportGenerator generator;
    private final HtmlReportRenderer htmlRenderer;

    public ReportController(ReportGenerator generator, HtmlReportRenderer htmlRenderer) {
        this.generator = generator;
        this.htmlRenderer = htmlRenderer;
    }

    /** Genera el reporte de un analisis completado. */
    @PostMapping
    public ResponseEntity<ReportDto> generate(@Valid @RequestBody ReportRequest request,
                                              @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        Report report = generator.generate(request.analysisId(), ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportDto.from(report));
    }

    @GetMapping
    public List<ReportSummaryDto> list(@RequestParam(required = false) String analysisId,
                                       @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        List<Report> reports = analysisId == null || analysisId.isBlank()
                ? generator.list(ownerId)
                : generator.listByAnalysis(analysisId);
        return reports.stream().map(ReportSummaryDto::from).toList();
    }

    @GetMapping("/{id}")
    public ReportDto get(@PathVariable String id) {
        return ReportDto.from(generator.get(id));
    }

    /** Reporte en Markdown, para pegarlo en un pull request o en la wiki. */
    @GetMapping(path = "/{id}/markdown", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<String> markdown(@PathVariable String id) {
        Report report = generator.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"codelens-" + slug(report.getProjectName()) + ".md\"")
                .body(report.getMarkdown());
    }

    /** Reporte en HTML autocontenido, imprimible a PDF desde el navegador. */
    @GetMapping(path = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> html(@PathVariable String id) {
        Report report = generator.get(id);
        return ResponseEntity.ok().body(htmlRenderer.render(report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        generator.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String slug(String value) {
        return value == null ? "reporte"
                : value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
