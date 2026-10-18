package com.rtbc.orders.application;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.ProductLine;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateOrder {

	private static final Logger log = LoggerFactory.getLogger(CreateOrder.class);

	private final OrderRepository repository;

	public CreateOrder(OrderRepository repository) {
		this.repository = repository;
	}

	public Order execute(String customer, List<ProductLine> products) {
		Order order = repository.save(Order.create(customer, products));
		log.info("Orden creada: id={} total={} productos={}",
				order.id(), order.total().toPlainString(), order.products().size());
		return order;
	}
}
