package com.codelens.user.service;

/** Usuario o contrasena incorrectos (se traduce a 401). */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
