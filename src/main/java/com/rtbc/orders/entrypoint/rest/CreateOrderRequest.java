package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.ProductLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request de creación. No incluye {@code total} ni {@code estado}: si llegan, se ignoran (ADR-007).
 */
record CreateOrderRequest(
		@NotBlank(message = "es obligatorio")
		@Size(max = 120, message = "admite máximo 120 caracteres")
		String cliente,

		@NotEmpty(message = "la orden debe contener al menos un producto")
		List<@NotNull(message = "no puede ser nulo") @Valid ProductLineDto> productos) {

	List<ProductLine> toProductLines() {
		return productos.stream().map(ProductLineDto::toDomain).toList();
	}
}
