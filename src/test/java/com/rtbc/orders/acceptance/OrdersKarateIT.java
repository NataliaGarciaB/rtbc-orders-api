package com.rtbc.orders.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de aceptación sobre la API desplegada (Karate). Se ejecutan con el perfil Maven {@code acceptance}.
 * Cada escenario crea sus propios datos, por eso corren en paralelo.
 */
@DisplayName("Pruebas de aceptación (Karate)")
class OrdersKarateIT {

	private static final int THREADS = 4;

	@Test
	@DisplayName("todos los escenarios de aceptación pasan")
	void escenariosDeAceptacion() {
		Runner.Builder<?> runner = Runner.path("classpath:acceptance").outputCucumberJson(false);
		String tags = System.getProperty("karate.tags", "");
		if (!tags.isBlank()) {
			runner.tags(tags);
		}

		Results results = runner.parallel(THREADS);

		assertThat(results.getScenariosTotal())
				.as("se ejecutó al menos un escenario (un filtro de tags mal escrito no debe dar falso verde)")
				.isPositive();
		assertThat(results.getFailCount()).as(results.getErrorMessages()).isZero();
	}
}
