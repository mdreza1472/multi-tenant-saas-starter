package com.reza.saas.common;

/** Thrown when a resource doesn't exist FOR THE CURRENT TENANT (mapped to HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
