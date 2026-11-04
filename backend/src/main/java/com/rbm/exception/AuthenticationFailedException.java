// exception/AuthenticationFailedException.java
package com.rbm.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Email ou mot de passe incorrect");
    }
}