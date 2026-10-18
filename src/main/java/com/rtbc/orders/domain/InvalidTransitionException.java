package com.rtbc.orders.domain;

/**
 * RN-04 / RN-06: transición no permitida desde el estado actual.
 */
public class InvalidTransitionException extends DomainException {

	public InvalidTransitionException(OrderStatus from, OrderStatus to) {
		super(ErrorCode.TRANSICION_INVALIDA, "No se puede pasar de " + from + " a " + to);
	}
}
