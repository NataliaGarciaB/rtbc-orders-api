package com.rtbc.orders.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados de la orden: única fuente de las transiciones permitidas (ADR-004).
 * Ver docs/maquina-estados.md.
 */
public enum OrderStatus {
	CREADA,
	PAGADA,
	ENVIADA,
	ENTREGADA,
	CANCELADA;

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
			CREADA, EnumSet.of(PAGADA, ENVIADA, CANCELADA),
			PAGADA, EnumSet.of(ENVIADA, CANCELADA),
			ENVIADA, EnumSet.of(ENTREGADA, CANCELADA),
			ENTREGADA, EnumSet.noneOf(OrderStatus.class),
			CANCELADA, EnumSet.noneOf(OrderStatus.class));

	/**
	 * @throws OrderNotPaidException       RN-05: CREADA → ENVIADA
	 * @throws InvalidTransitionException RN-04 / RN-06: cualquier otra transición no permitida
	 */
	public void ensureCanTransitionTo(OrderStatus target) {
		if (ALLOWED_TRANSITIONS.get(this).contains(target)) {
			return;
		}
		if (this == CREADA && target == ENVIADA) {
			throw new OrderNotPaidException();
		}
		throw new InvalidTransitionException(this, target);
	}
}
