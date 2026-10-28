package com.codelens.user.service;

/** El usuario o el correo ya estan registrados (se traduce a 409). */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
