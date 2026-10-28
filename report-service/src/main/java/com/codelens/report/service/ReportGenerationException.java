package com.codelens.report.service;

/** El reporte no se pudo generar por una condicion de negocio. */
public class ReportGenerationException extends RuntimeException {

    public ReportGenerationException(String message) {
        super(message);
    }
}
