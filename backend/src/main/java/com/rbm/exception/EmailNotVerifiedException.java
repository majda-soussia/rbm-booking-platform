package com.rbm.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Adresse email non vérifiée : cliquez sur le lien reçu par email, ou demandez un nouvel email.");
    }
}