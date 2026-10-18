package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.ProductLine;
import java.util.List;

/**
 * Request de creación. No incluye {@code total} ni {@code estado}: si llegan, se ignoran (ADR-007).
 */
record CreateOrderRequest(String cliente, List<ProductLineDto> productos) {

	List<ProductLine> toProductLines() {
		return productos.stream().map(ProductLineDto::toDomain).toList();
	}
}
