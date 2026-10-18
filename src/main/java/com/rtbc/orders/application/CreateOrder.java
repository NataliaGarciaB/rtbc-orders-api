package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.ProductLine;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateOrder {

	private final OrderRepository repository;

	public CreateOrder(OrderRepository repository) {
		this.repository = repository;
	}

	public Order execute(String customer, List<ProductLine> products) {
		return repository.save(Order.create(customer, products));
	}
}
