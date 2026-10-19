package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static com.rtbc.orders.domain.OrderFixtures.orderIn;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DEMO del quality gate (PR que no se fusiona): la prueba ejecuta todas las ramas, así que la cobertura pasa,
 * pero no verifica ningún resultado. El mutation testing lo detecta.
 */
@DisplayName("DEMO: prueba que ejecuta pero no verifica")
class DiscountPolicyTest {

	private final DiscountPolicy policy = new DiscountPolicy();

	@Test
	@DisplayName("calcula descuentos (sin aserciones)")
	void calculaDescuentos() {
		for (int units : new int[] { 1, 10, 50, 100 }) {
			policy.discountFor(Order.create(CUSTOMER, List.of(line("P001", units, "100"))));
		}
		List<ProductLine> manyLines = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			manyLines.add(line("P00" + i, 20, "10000"));
		}
		policy.discountFor(Order.create(CUSTOMER, manyLines));
		policy.discountFor(orderIn(OrderStatus.CANCELADA));
		policy.discountFor(orderIn(OrderStatus.ENTREGADA));
		Order large = Order.create(CUSTOMER, manyLines);
		Order single = Order.create(CUSTOMER, List.of(line("P001", 1, "600000")));
		Order small = Order.create(CUSTOMER, List.of(line("P001", 1, "100"), line("P002", 1, "100")));
		for (Order order : List.of(large, single, small)) {
			policy.totalWithDiscount(order);
			policy.qualifiesForFreeShipping(order);
		}
	}
}
