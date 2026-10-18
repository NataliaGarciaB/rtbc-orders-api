package com.rtbc.orders.application;

import com.rtbc.orders.domain.DomainException;
import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderStatus;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica una transición de estado; las reglas viven en el dominio (ADR-004).
 */
@Service
@Transactional
public class ChangeOrderStatus {

	private static final Logger log = LoggerFactory.getLogger(ChangeOrderStatus.class);

	private final OrderRepository repository;

	public ChangeOrderStatus(OrderRepository repository) {
		this.repository = repository;
	}

	public Order execute(UUID id, OrderStatus target) {
		Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
		OrderStatus from = order.status();
		try {
			order.changeStatus(target);
		} catch (DomainException e) {
			log.warn("Transición rechazada: id={} de={} a={} codigo={}", id, from, target, e.code());
			throw e;
		}
		Order saved = repository.save(order);
		log.info("Estado cambiado: id={} de={} a={}", id, from, target);
		return saved;
	}
}
