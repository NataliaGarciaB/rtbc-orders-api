package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetOrder {

	private final OrderRepository repository;

	public GetOrder(OrderRepository repository) {
		this.repository = repository;
	}

	public Order execute(UUID id) {
		return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}
}
