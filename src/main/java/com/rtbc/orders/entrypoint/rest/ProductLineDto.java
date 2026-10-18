package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.domain.ProductLine;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Línea de producto con los nombres de campo del contrato (docs/api/openapi.yaml).
 * Las validaciones repiten las del dominio en el borde HTTP para reportar todos los errores a la vez (ADR-008).
 */
record ProductLineDto(
		@NotBlank(message = "es obligatorio")
		@Size(max = 50, message = "admite máximo 50 caracteres")
		String id,

		@NotNull(message = "es obligatoria")
		@Positive(message = "debe ser mayor que cero")
		Integer cantidad,

		@NotNull(message = "es obligatorio")
		@DecimalMin(value = "0.01", message = "debe ser mayor que cero")
		// 17 enteros + 2 decimales = columna unit_price (precision 19, scale 2)
		@Digits(integer = 17, fraction = 2, message = "admite máximo 17 enteros y 2 decimales")
		BigDecimal valorUnitario) {

	static ProductLineDto from(ProductLine line) {
		return new ProductLineDto(line.productId(), line.quantity(), line.unitPrice());
	}

	ProductLine toDomain() {
		return new ProductLine(id, cantidad, valorUnitario);
	}
}
