package com.athenet.social.exception;

// Cognito u otro servicio EX. no respondió o respondió algo inesperado.
public class UpstreamServiceException extends RuntimeException{
    
    public UpstreamServiceException(String message) {
        super(message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
