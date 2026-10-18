package com.rtbc.orders.domain;

/**
 * RN-05: se intentó enviar una orden que no ha sido pagada.
 */
public class OrderNotPaidException extends DomainException {

	public OrderNotPaidException() {
		super(ErrorCode.ORDEN_NO_PAGADA, "La orden no puede ser enviada si no ha sido pagada");
	}
}
