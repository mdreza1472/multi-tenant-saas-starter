package com.reza.saas.common;

/** Uniform error body returned by {@link GlobalExceptionHandler}. */
public record ApiError(int status, String error, String message) {
}
