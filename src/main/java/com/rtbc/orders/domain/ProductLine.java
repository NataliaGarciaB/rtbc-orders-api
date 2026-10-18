package com.rtbc.orders.domain;

import java.math.BigDecimal;

public record ProductLine(String productId, int quantity, BigDecimal unitPrice) {

	public ProductLine {
		if (quantity <= 0) {
			throw new ValidationException("productos.cantidad", "la cantidad debe ser mayor que cero");
		}
	}
}
