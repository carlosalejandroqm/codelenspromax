package com.codelens.source.service;

/** Error de negocio durante la obtencion del codigo (se traduce a 4xx/502). */
public class SourceIngestionException extends RuntimeException {

    public SourceIngestionException(String message) {
        super(message);
    }

    public SourceIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
