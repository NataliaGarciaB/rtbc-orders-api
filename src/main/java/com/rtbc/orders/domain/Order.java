package com.rtbc.orders.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {

	static final int CUSTOMER_MAX_LENGTH = 120;

	private final UUID id;
	private final String customer;
	private final List<ProductLine> products;
	private OrderStatus status;

	private Order(UUID id, String customer, List<ProductLine> products, OrderStatus status) {
		this.id = id;
		this.customer = customer;
		this.products = products;
		this.status = status;
	}

	public static Order create(String customer, List<ProductLine> products) {
		validateCustomer(customer);
		validateProducts(products);
		return new Order(UUID.randomUUID(), customer, List.copyOf(products), OrderStatus.CREADA);
	}

	private static void validateCustomer(String customer) {
		if (customer == null || customer.isBlank()) {
			throw new ValidationException("cliente", "el cliente es obligatorio");
		}
		if (customer.length() > CUSTOMER_MAX_LENGTH) {
			throw new ValidationException("cliente",
					"el cliente admite máximo " + CUSTOMER_MAX_LENGTH + " caracteres");
		}
	}

	private static void validateProducts(List<ProductLine> products) {
		if (products == null || products.isEmpty()) {
			throw new ValidationException("productos", "la orden debe contener al menos un producto");
		}
		if (products.stream().anyMatch(Objects::isNull)) {
			throw new ValidationException("productos", "la orden no puede contener productos nulos");
		}
	}

	public UUID id() {
		return id;
	}

	public String customer() {
		return customer;
	}

	public List<ProductLine> products() {
		return products;
	}

	public OrderStatus status() {
		return status;
	}

	public void changeStatus(OrderStatus target) {
		if (target == null) {
			throw new ValidationException("estado", "el estado es obligatorio");
		}
		boolean allowed = switch (status) {
			case CREADA -> target == OrderStatus.PAGADA || target == OrderStatus.CANCELADA;
			case PAGADA -> target == OrderStatus.ENVIADA || target == OrderStatus.CANCELADA;
			case ENVIADA -> target == OrderStatus.ENTREGADA || target == OrderStatus.CANCELADA;
			case ENTREGADA, CANCELADA -> false;
		};
		if (!allowed && status == OrderStatus.CREADA && target == OrderStatus.ENVIADA) {
			throw new OrderNotPaidException();
		}
		if (!allowed) {
			throw new InvalidTransitionException(status, target);
		}
		status = target;
	}

	/**
	 * RN-03: siempre calculado a partir de las líneas; nunca se recibe del cliente (ADR-007).
	 */
	public BigDecimal total() {
		return products.stream()
				.map(ProductLine::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
