package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListOrders {

	private final OrderRepository repository;

	public ListOrders(OrderRepository repository) {
		this.repository = repository;
	}

	public List<Order> execute() {
		return repository.findAll();
	}
}
