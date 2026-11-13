package com.athenet.social.exception;

// Error al no encontrar un recurso buscando por su ID.
public class ResourceNotFoundException  extends RuntimeException {

    public ResourceNotFoundException(String message) { 
        super(message);
    }
}
