package com.codelens.user.service;

/** El usuario solicitado no existe (se traduce a 404). */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
