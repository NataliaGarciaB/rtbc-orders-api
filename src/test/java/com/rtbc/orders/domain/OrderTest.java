package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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

		@Test
		@DisplayName("rechaza una lista de productos con un elemento nulo")
		void rechazaProductoNulo() {
			List<ProductLine> products = Arrays.asList(line(), null);

			assertThatThrownBy(() -> Order.create(CUSTOMER, products))
					.isInstanceOfSatisfying(ValidationException.class,
							e -> assertThat(e.field()).isEqualTo("productos"));
		}

		@Test
		@DisplayName("no se ve afectada si la lista original cambia después de crearla")
		void copiaDefensivaDeProductos() {
			List<ProductLine> products = new ArrayList<>(List.of(line()));
			Order order = Order.create(CUSTOMER, products);

			products.add(line());

			assertThat(order.products()).hasSize(1);
		}
	}

	@Nested
	@Tag("RN-03")
	@DisplayName("RN-03: el total se calcula automáticamente")
	class Total {

		@Test
		@DisplayName("una línea: 2 × 100000 = 200000 (ejemplo del enunciado)")
		void unaLinea() {
			Order order = Order.create(CUSTOMER, List.of(line("P001", 2, "100000")));

			assertThat(order.total()).isEqualByComparingTo("200000");
		}

		@Test
		@DisplayName("varias líneas: suma de cantidad × valor unitario")
		void variasLineas() {
			Order order = Order.create(CUSTOMER, List.of(
					line("P001", 2, "100000"),
					line("P002", 1, "50000"),
					line("P003", 3, "1500")));

			assertThat(order.total()).isEqualByComparingTo("254500");
		}

		@Test
		@DisplayName("decimales sin error de redondeo: 3 × 10.50 + 1 × 0.99 = 32.49")
		void decimalesExactos() {
			Order order = Order.create(CUSTOMER, List.of(
					line("P001", 3, "10.50"),
					line("P002", 1, "0.99")));

			assertThat(order.total()).isEqualByComparingTo("32.49");
		}

		@Test
		@DisplayName("valores grandes sin desbordamiento")
		void valoresGrandes() {
			Order order = Order.create(CUSTOMER, List.of(
					line("P001", Integer.MAX_VALUE, "999999999.99")));

			assertThat(order.total()).isEqualByComparingTo("2147483646978525163.53");
		}

		@Test
		@DisplayName("el subtotal de una línea es cantidad × valor unitario")
		void subtotalDeLinea() {
			assertThat(line("P001", 4, "2.25").subtotal()).isEqualByComparingTo("9.00");
		}
	}

	@Nested
	@Tag("validaciones")
	@DisplayName("ADR-008: cliente obligatorio, máximo 120 caracteres")
	class Cliente {

		@ParameterizedTest(name = "rechaza cliente \"{0}\"")
		@NullSource
		@ValueSource(strings = { "", "   " })
		void rechazaClienteVacio(String customer) {
			assertThatThrownBy(() -> Order.create(customer, List.of(line())))
					.isInstanceOfSatisfying(ValidationException.class,
							e -> assertThat(e.field()).isEqualTo("cliente"));
		}

		@Test
		@DisplayName("rechaza cliente de 121 caracteres")
		void rechazaClienteDemasiadoLargo() {
			assertThatThrownBy(() -> Order.create("a".repeat(121), List.of(line())))
					.isInstanceOfSatisfying(ValidationException.class,
							e -> assertThat(e.field()).isEqualTo("cliente"));
		}

		@ParameterizedTest(name = "acepta cliente de {0} caracteres")
		@ValueSource(ints = { 1, 120 })
		void aceptaLimitesValidos(int length) {
			assertThat(Order.create("a".repeat(length), List.of(line())).customer()).hasSize(length);
		}
	}
}
