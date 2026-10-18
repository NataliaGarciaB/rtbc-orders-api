package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Línea de producto")
class ProductLineTest {

	@Nested
	@Tag("RN-02")
	@DisplayName("RN-02: la cantidad debe ser mayor que cero")
	class CantidadMayorQueCero {

		@ParameterizedTest(name = "rechaza cantidad {0}")
		@ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
		void rechazaCantidadNoPositiva(int quantity) {
			assertThatThrownBy(() -> line("P001", quantity, "100"))
					.isInstanceOfSatisfying(ValidationException.class, e -> {
						assertThat(e.code()).isEqualTo(ErrorCode.VALIDACION);
						assertThat(e.field()).isEqualTo("productos.cantidad");
					});
		}

		@Test
		@DisplayName("acepta cantidad 1 (límite inferior válido)")
		void aceptaCantidadUno() {
			assertThat(line("P001", 1, "100").quantity()).isEqualTo(1);
		}
	}
}
