package com.athenet.social.exception;

// Errores de negocio (duplicados y estados inválidos)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { 
        super(message);
    }
}
