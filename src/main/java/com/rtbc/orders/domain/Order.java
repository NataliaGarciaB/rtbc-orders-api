package com.rtbc.orders.domain;

import java.util.List;
import java.util.UUID;

public final class Order {

	private final UUID id;
	private final String customer;
	private final List<ProductLine> products;
	private final OrderStatus status;

	private Order(UUID id, String customer, List<ProductLine> products, OrderStatus status) {
		this.id = id;
		this.customer = customer;
		this.products = products;
		this.status = status;
	}

	public static Order create(String customer, List<ProductLine> products) {
		if (products == null || products.isEmpty()) {
			throw new ValidationException("productos", "la orden debe contener al menos un producto");
		}
		return new Order(UUID.randomUUID(), customer, List.copyOf(products), OrderStatus.CREADA);
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
}
