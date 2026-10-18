package com.rtbc.orders.infrastructure.persistence;

import com.rtbc.orders.application.OrderRepository;
import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.ProductLine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador del puerto {@link OrderRepository} sobre JPA/H2 (ADR-003).
 */
@Repository
public class JpaOrderRepository implements OrderRepository {

	private final SpringDataOrderRepository jpa;

	JpaOrderRepository(SpringDataOrderRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Order save(Order order) {
		OrderEntity entity = jpa.findById(order.id())
				.map(existing -> {
					existing.changeStatus(order.status());
					return existing;
				})
				.orElseGet(() -> toEntity(order));
		return toDomain(jpa.save(entity));
	}

	@Override
	public Optional<Order> findById(UUID id) {
		return jpa.findById(id).map(JpaOrderRepository::toDomain);
	}

	@Override
	public List<Order> findAll() {
		return jpa.findAll().stream().map(JpaOrderRepository::toDomain).toList();
	}

	private static OrderEntity toEntity(Order order) {
		List<OrderLineEmbeddable> lines = order.products().stream()
				.map(p -> new OrderLineEmbeddable(p.productId(), p.quantity(), p.unitPrice()))
				.toList();
		return new OrderEntity(order.id(), order.customer(), lines, order.status());
	}

	private static Order toDomain(OrderEntity entity) {
		List<ProductLine> products = entity.lines().stream()
				.map(l -> new ProductLine(l.productId(), l.quantity(), l.unitPrice()))
				.toList();
		return Order.restore(entity.id(), entity.customer(), products, entity.status());
	}
}
