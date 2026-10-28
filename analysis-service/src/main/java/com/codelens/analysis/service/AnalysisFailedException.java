package com.codelens.analysis.service;

/** El analisis no se pudo completar por una condicion de negocio. */
public class AnalysisFailedException extends RuntimeException {

    public AnalysisFailedException(String message) {
        super(message);
    }
}
