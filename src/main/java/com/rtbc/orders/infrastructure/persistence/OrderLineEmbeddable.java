package com.rtbc.orders.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
class OrderLineEmbeddable {

	@Column(name = "product_id", nullable = false, length = 50)
	private String productId;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	protected OrderLineEmbeddable() {
	}

	OrderLineEmbeddable(String productId, int quantity, BigDecimal unitPrice) {
		this.productId = productId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	String productId() {
		return productId;
	}

	int quantity() {
		return quantity;
	}

	BigDecimal unitPrice() {
		return unitPrice;
	}
}
