package com.rtbc.orders.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DEMO del quality gate (PR que no se fusiona): regla de descuentos agregada sin pruebas suficientes.
 */
public final class DiscountPolicy {

	private static final BigDecimal MAX_RATE = new BigDecimal("0.17");
	private static final BigDecimal LARGE_ORDER = new BigDecimal("1000000");
	private static final BigDecimal FREE_SHIPPING = new BigDecimal("500000");

	public BigDecimal discountFor(Order order) {
		if (order.status() == OrderStatus.CANCELADA || order.status() == OrderStatus.ENTREGADA) {
			return BigDecimal.ZERO;
		}
		int units = order.products().stream().mapToInt(ProductLine::quantity).sum();
		BigDecimal rate = BigDecimal.ZERO;
		if (units >= 100) {
			rate = new BigDecimal("0.15");
		} else if (units >= 50) {
			rate = new BigDecimal("0.10");
		} else if (units >= 10) {
			rate = new BigDecimal("0.05");
		}
		if (order.total().compareTo(LARGE_ORDER) > 0) {
			rate = rate.add(new BigDecimal("0.02"));
		}
		if (order.products().size() > 5) {
			rate = rate.add(new BigDecimal("0.01"));
		}
		if (rate.compareTo(MAX_RATE) > 0) {
			rate = MAX_RATE;
		}
		return order.total().multiply(rate).setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal totalWithDiscount(Order order) {
		BigDecimal result = order.total().subtract(discountFor(order));
		return result.signum() < 0 ? BigDecimal.ZERO : result;
	}

	public boolean qualifiesForFreeShipping(Order order) {
		return order.total().compareTo(FREE_SHIPPING) >= 0 && order.products().size() > 1;
	}
}
