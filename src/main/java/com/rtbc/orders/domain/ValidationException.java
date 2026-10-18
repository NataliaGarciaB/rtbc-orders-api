package com.rtbc.orders.domain;

/**
 * Dato de entrada inválido. {@code field} usa el nombre del campo en el contrato.
 */
public class ValidationException extends DomainException {

	private final String field;

	public ValidationException(String field, String message) {
		super(ErrorCode.VALIDACION, message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
