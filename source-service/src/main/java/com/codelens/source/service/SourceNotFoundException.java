package com.codelens.source.service;

/** El snapshot o el archivo solicitado no existe (se traduce a 404). */
public class SourceNotFoundException extends RuntimeException {

    public SourceNotFoundException(String message) {
        super(message);
    }
}
