package com.rtbc.orders.entrypoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI publica el contrato de diseño (docs/api/openapi.yaml) tal cual: una sola fuente de verdad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Documentación de la API")
class ApiDocumentationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("publica docs/api/openapi.yaml sin modificaciones")
	void publicaContratoDeDiseno() throws Exception {
		String contract = Files.readString(Path.of("docs/api/openapi.yaml"), StandardCharsets.UTF_8);

		mvc.perform(get("/openapi.yaml"))
				.andExpect(status().isOk())
				.andExpect(content().string(contract));
	}

	@Test
	@DisplayName("Swagger UI está disponible y apunta al contrato de diseño")
	void swaggerUiApuntaAlContrato() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs/swagger-config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("/openapi.yaml"));
	}

	@Test
	@DisplayName("no publica una especificación generada que compita con el contrato")
	void sinEspecificacionGenerada() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
	}
}
