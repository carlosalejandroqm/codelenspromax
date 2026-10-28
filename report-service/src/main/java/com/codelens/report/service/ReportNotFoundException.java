package com.codelens.report.service;

/** El reporte solicitado no existe (se traduce a 404). */
public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String message) {
        super(message);
    }
}
