package com.codelens.report.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Analisis con sus hallazgos, tal y como lo entrega /analyses/{id}/detail. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisDetailView(
        AnalysisView analysis,
        List<FindingView> findings
) {
}
