package com.rtbc.orders.domain;

import java.math.BigDecimal;

public record ProductLine(String productId, int quantity, BigDecimal unitPrice) {

	static final int PRODUCT_ID_MAX_LENGTH = 50;
	static final int UNIT_PRICE_MAX_DECIMALS = 2;

	public ProductLine {
		if (productId == null || productId.isBlank()) {
			throw new ValidationException("productos.id", "el id del producto es obligatorio");
		}
		if (productId.length() > PRODUCT_ID_MAX_LENGTH) {
			throw new ValidationException("productos.id",
					"el id del producto admite máximo " + PRODUCT_ID_MAX_LENGTH + " caracteres");
		}
		if (quantity <= 0) {
			throw new ValidationException("productos.cantidad", "la cantidad debe ser mayor que cero");
		}
		if (unitPrice == null || unitPrice.signum() <= 0) {
			throw new ValidationException("productos.valorUnitario", "el valor unitario debe ser mayor que cero");
		}
		if (unitPrice.stripTrailingZeros().scale() > UNIT_PRICE_MAX_DECIMALS) {
			throw new ValidationException("productos.valorUnitario",
					"el valor unitario admite máximo " + UNIT_PRICE_MAX_DECIMALS + " decimales");
		}
	}

	public BigDecimal subtotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}
