package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record OrderResponse(UUID id, String cliente, List<ProductLineDto> productos, BigDecimal total, OrderStatus estado) {

	static OrderResponse from(Order order) {
		return new OrderResponse(
				order.id(),
				order.customer(),
				order.products().stream().map(ProductLineDto::from).toList(),
				order.total(),
				order.status());
	}
}
