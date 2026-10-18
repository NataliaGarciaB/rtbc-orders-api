package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica una transición de estado; las reglas viven en el dominio (ADR-004).
 */
@Service
@Transactional
public class ChangeOrderStatus {

	private final OrderRepository repository;

	public ChangeOrderStatus(OrderRepository repository) {
		this.repository = repository;
	}

	public Order execute(UUID id, OrderStatus target) {
		Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
		order.changeStatus(target);
		return repository.save(order);
	}
}
