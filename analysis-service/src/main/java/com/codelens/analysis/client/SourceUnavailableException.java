package com.codelens.analysis.client;

/** El microservicio de codigo fuente no respondio o no tiene el snapshot. */
public class SourceUnavailableException extends RuntimeException {

    public SourceUnavailableException(String message) {
        super(message);
    }

    public SourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
