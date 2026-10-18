package com.rtbc.orders.domain;

/**
 * Base de los errores de negocio: cada uno lleva un {@link ErrorCode} estable.
 */
public abstract class DomainException extends RuntimeException {

	private final ErrorCode code;

	protected DomainException(ErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public ErrorCode code() {
		return code;
	}
}
