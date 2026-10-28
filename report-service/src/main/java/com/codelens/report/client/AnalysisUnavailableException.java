package com.codelens.report.client;

/** El microservicio de analisis no respondio o no tiene el analisis pedido. */
public class AnalysisUnavailableException extends RuntimeException {

    public AnalysisUnavailableException(String message) {
        super(message);
    }

    public AnalysisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
