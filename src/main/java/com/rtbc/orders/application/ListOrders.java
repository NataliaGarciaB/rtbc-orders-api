package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import java.util.List;

public class ListOrders {

	private final OrderRepository repository;

	public ListOrders(OrderRepository repository) {
		this.repository = repository;
	}

	public List<Order> execute() {
		return repository.findAll();
	}
}
