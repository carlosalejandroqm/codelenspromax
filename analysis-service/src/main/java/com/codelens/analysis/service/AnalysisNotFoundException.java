package com.codelens.analysis.service;

/** El analisis solicitado no existe (se traduce a 404). */
public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(String message) {
        super(message);
    }
}
