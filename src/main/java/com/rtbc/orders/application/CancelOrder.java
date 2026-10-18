package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderStatus;
import java.util.UUID;

/**
 * Cancelación lógica: misma regla que actualizar el estado a CANCELADA (ADR-006).
 */
public class CancelOrder {

	private final ChangeOrderStatus changeOrderStatus;

	public CancelOrder(ChangeOrderStatus changeOrderStatus) {
		this.changeOrderStatus = changeOrderStatus;
	}

	public Order execute(UUID id) {
		return changeOrderStatus.execute(id, OrderStatus.CANCELADA);
	}
}
