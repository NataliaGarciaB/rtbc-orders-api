package com.rtbc.orders.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de prueba legibles: cada prueba parte de una orden válida y cambia solo lo que prueba.
 */
public final class OrderFixtures {

	public static final String CUSTOMER = "Juan Pérez";

	private OrderFixtures() {
	}

	public static ProductLine line() {
		return line("P001", 2, "100000");
	}

	public static ProductLine line(String productId, int quantity, String unitPrice) {
		return new ProductLine(productId, quantity, new BigDecimal(unitPrice));
	}

	public static Order validOrder() {
		return Order.create(CUSTOMER, List.of(line()));
	}
}
