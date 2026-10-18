package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de órdenes. La implementación vive en {@code infrastructure} (ADR-002).
 */
public interface OrderRepository {

	Order save(Order order);

	Optional<Order> findById(UUID id);

	List<Order> findAll();
}
