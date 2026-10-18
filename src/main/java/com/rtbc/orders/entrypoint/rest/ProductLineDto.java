package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.ProductLine;
import java.math.BigDecimal;

/**
 * Línea de producto con los nombres de campo del contrato (docs/api/openapi.yaml).
 */
record ProductLineDto(String id, Integer cantidad, BigDecimal valorUnitario) {

	static ProductLineDto from(ProductLine line) {
		return new ProductLineDto(line.productId(), line.quantity(), line.unitPrice());
	}

	ProductLine toDomain() {
		return new ProductLine(id, cantidad, valorUnitario);
	}
}
