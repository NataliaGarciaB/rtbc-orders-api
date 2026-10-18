package com.rtbc.orders.entrypoint.rest;

/**
 * Elemento de la lista {@code errores} de un ProblemDetail de validación (ADR-009).
 */
record FieldErrorResponse(String campo, String mensaje) {
}
