package com.rtbc.orders.domain;

import java.math.BigDecimal;

public record ProductLine(String productId, int quantity, BigDecimal unitPrice) {
}
