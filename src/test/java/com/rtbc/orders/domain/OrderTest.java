package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Orden")
class OrderTest {

	@Nested
	@Tag("RN-01")
	@DisplayName("RN-01: la orden debe contener al menos un producto")
	class AlMenosUnProducto {

		@Test
		@DisplayName("rechaza una orden con la lista de productos vacía")
		void rechazaListaVacia() {
			assertThatThrownBy(() -> Order.create(CUSTOMER, List.of()))
					.isInstanceOfSatisfying(ValidationException.class, e -> {
						assertThat(e.code()).isEqualTo(ErrorCode.VALIDACION);
						assertThat(e.field()).isEqualTo("productos");
					});
		}

		@Test
		@DisplayName("rechaza una orden sin lista de productos")
		void rechazaListaNula() {
			assertThatThrownBy(() -> Order.create(CUSTOMER, null))
					.isInstanceOfSatisfying(ValidationException.class,
							e -> assertThat(e.field()).isEqualTo("productos"));
		}

		@Test
		@DisplayName("crea la orden en estado CREADA con un producto")
		void creaOrdenConUnProducto() {
			ProductLine product = line();

			Order order = Order.create(CUSTOMER, List.of(product));

			assertThat(order.id()).isNotNull();
			assertThat(order.customer()).isEqualTo(CUSTOMER);
			assertThat(order.products()).containsExactly(product);
			assertThat(order.status()).isEqualTo(OrderStatus.CREADA);
		}

		@Test
		@DisplayName("genera un id distinto para cada orden")
		void generaIdUnico() {
			assertThat(OrderFixtures.validOrder().id()).isNotEqualTo(OrderFixtures.validOrder().id());
		}
	}
}
