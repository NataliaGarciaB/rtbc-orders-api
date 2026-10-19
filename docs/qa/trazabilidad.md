# Matriz de trazabilidad: requisitos → riesgos → pruebas

Conecta cada requisito del reto con su riesgo y con las pruebas que lo cubren. Se actualiza al cerrar cada fase. Estado: 📝 planificado · 🟡 parcial (niveles bajos implementados) · ✅ completo.

**Convención de etiquetas** (permite filtrar la ejecución por regla):
- JUnit: `@Tag("RN-01")` → `./mvnw test -Dgroups=RN-01`
- Karate: `@RN-01` → `./mvnw verify -Pacceptance -Dkarate.tags=@RN-01`

## Reglas de negocio

| Req. | Descripción | Riesgo | Unitarias (dominio / caso de uso) | Integración (`@WebMvcTest`) | Karate | E2E | Estado |
|---|---|---|---|---|---|---|---|
| RN-01 | La orden debe tener al menos un producto | R03 | ✅ `OrderTest.AlMenosUnProducto`: lista vacía, nula y con elemento nulo se rechazan; un producto crea la orden en CREADA. `CreateOrderTest`: no persiste si es inválida | ✅ `RequestValidationTest.sinProductos`, `productoNulo` → 400 `VALIDACION` | ✅ `validaciones.feature` @RN-01 | — | ✅ Fase 4 |
| RN-02 | La cantidad debe ser mayor que 0 | R03 | ✅ `ProductLineTest.CantidadMayorQueCero`: MIN_VALUE, −1 y 0 se rechazan; 1 se acepta | ✅ `RequestValidationTest.cantidadCero` (`productos[0].cantidad`), `cantidadDecimal` (1.5 → 400, no se trunca) | ✅ `validaciones.feature` @RN-02 | — | ✅ Fase 4 |
| RN-03 | Total calculado automáticamente | R02 | ✅ `OrderTest.Total`: una línea (ejemplo del PDF), varias líneas, decimales exactos (32.49), valores grandes sin desbordamiento, subtotal | ✅ `OrderControllerTest.ignoraTotalYEstado`; `decimalesSinNotacionCientifica`. `JpaOrderRepositoryTest`: el total se conserva tras persistir | ✅ `crear.feature` @RN-03: total enviado ignorado; varias líneas con decimales persistidas | Crear orden muestra el total | 🟡 API ✅ · E2E Fase 8b |
| RN-04 | Una orden entregada no se puede cancelar | R01 | ✅ `OrderStatusTransitionTest.ordenEntregadaNoSeCancela`; `CancelOrderTest.noCancelaOrdenEntregada` (no persiste) | ✅ `ErrorHandlingTest.cancelarEntregada` → 409 `TRANSICION_INVALIDA` | ✅ ⭐ `transiciones.feature` @RN-04: cancelar una orden entregada → 409 y la orden no cambia | — | ✅ Fase 4 |
| RN-05 | No se puede enviar sin pagar | R01 | ✅ `OrderStatusTransitionTest.ordenNoPagadaNoSeEnvia`; `ChangeOrderStatusTest.enviarSinPagar` (no persiste) | ✅ `ErrorHandlingTest.ordenNoPagada` → 400 `ORDEN_NO_PAGADA` | ✅ ⭐ `transiciones.feature` @RN-05: enviar una orden no pagada → 400 | — | ✅ Fase 4 |
| RN-06 | No se permiten transiciones inválidas | R01, R08 | ✅ `OrderStatusTransitionTest.matrizDeTransiciones`: 25 combinaciones de [maquina-estados.md](../maquina-estados.md), verifica que el estado no cambia al rechazar; flujo completo; destino nulo. `ChangeOrderStatusTest` | ✅ `ErrorHandlingTest.transicionInvalida` → 409; `RequestValidationTest.estadoDesconocido`, `estadoAusente` → 400 | ✅ `transiciones.feature` @RN-06: 8 transiciones de muestra y flujo completo; `cancelar.feature`: cancelar una orden cancelada → 409 | Actualizar estado | 🟡 API ✅ · E2E Fase 8b |

## Endpoints y escenarios obligatorios del PDF

| Endpoint | Escenario | Código | Riesgo | Karate | Integración | E2E | Estado |
|---|---|---|---|---|---|---|---|
| `POST /orders` | **Crear una orden válida** (obligatorio) | 201 + `Location` | R02, R03 | ✅ ⭐ `crear.feature` | ✅ `OrderControllerTest.Crear` | Crear orden | 🟡 API ✅ · E2E Fase 8b |
| `GET /orders/{id}` | **Consultar una orden existente** (obligatorio) | 200 | R04 | ✅ ⭐ `consultar.feature` | ✅ `OrderControllerTest.Consultar` | Consultar orden | 🟡 API ✅ · E2E Fase 8b |
| `GET /orders/{id}` | Orden inexistente | 404 | R05 | ✅ `consultar.feature` | ✅ `ErrorHandlingTest.ordenInexistente`; `GetOrderTest` | — | ✅ Fase 4 |
| `GET /orders/{id}` | UUID mal formado | 400 | R05 | ✅ `validaciones.feature` | ✅ `RequestValidationTest.idMalFormado` (campo `id`) | — | ✅ Fase 4 |
| `GET /orders` | Listar órdenes | 200 | R04 | ✅ `consultar.feature` @listar | ✅ `OrderControllerTest.Listar` (con datos y vacía) | — | ✅ Fase 4 |
| `PATCH /orders/{id}/status` | Transición válida | 200 | R01 | ✅ `transiciones.feature` @RN-06 | ✅ `OrderControllerTest.ActualizarEstado` | Actualizar estado | 🟡 API ✅ · E2E Fase 8b |
| `PATCH /orders/{id}/status` | Estado desconocido o ausente | 400 | R05 | ✅ `validaciones.feature` | ✅ `RequestValidationTest.ActualizarEstado` | — | ✅ Fase 4 |
| `DELETE /orders/{id}` | Cancelar una orden creada | 200 | R01 | ✅ `cancelar.feature`: cancelación lógica, sigue consultable | ✅ `OrderControllerTest.Cancelar` | Cancelar orden | 🟡 API ✅ · E2E Fase 8b |

## Validaciones adicionales (ADR-008, ADR-020)

| Validación | Riesgo | Unitarias / Integración | Karate | Estado |
|---|---|---|---|---|
| `cliente` obligatorio, entre 1 y 120 caracteres | R03 | ✅ `OrderTest.Cliente` (nulo, vacío, espacios, 121 / 1 y 120). ✅ `RequestValidationTest.camposObligatorios`, `longitudesMaximas` | ✅ `validaciones.feature` cliente vacío | ✅ Fase 4 |
| `productos[].id` obligatorio, entre 1 y 50 caracteres | R03 | ✅ `ProductLineTest.IdDeProducto` (nulo, vacío, espacios, 51 / 1 y 50). ✅ `RequestValidationTest.camposObligatorios`, `longitudesMaximas` | — | ✅ Fase 3 |
| `valorUnitario` > 0, máximo 2 decimales y 17 enteros | R02, R03 | ✅ `ProductLineTest.ValorUnitario`. ✅ `RequestValidationTest.valorUnitarioInvalido` (0, −1, 10.555, 10^17) | ✅ `validaciones.feature` valor 0 y 3 decimales | ✅ Fase 4 |
| Todos los errores de campo se reportan juntos | R04 | ✅ `RequestValidationTest.variosErrores` | — | ✅ Fase 3 |
| JSON mal formado o sin body | R05 | ✅ `RequestValidationTest.jsonMalFormado`, `sinBody` → 400 `VALIDACION` | — | ✅ Fase 3 |
| Un request inválido nunca llega a los casos de uso | R03 | ✅ `RequestValidationTest` verifica en cada caso que no hubo interacción con los casos de uso | — | ✅ Fase 3 |
| Sin fuga de información en errores | R06 | ✅ `UnexpectedErrorTest`: 500 `ERROR_INTERNO` sin mensaje, clase ni traza; la excepción completa queda en el log | — | ✅ Fase 3 |

## Requisitos no funcionales

| Requisito | Riesgo | Verificación | Estado |
|---|---|---|---|
| Dependencias entre capas | R10 | ArchUnit: `ArchitectureTest` (2 reglas) | ✅ Fase 1 |
| Cobertura ≥ 80% de líneas y ramas | R09 | JaCoCo `check` en `verify` (local y job `build` en CI). Fase 3: 100% líneas (249/249), 100% ramas (57/57) | ✅ Fase 2-3 |
| Mutation score ≥ 70% | R09 | PIT `mutationThreshold` con mutadores STRONGER sobre `domain` y `application`. Fase 3: 66/66 mutantes eliminados (100%). Job `mutation` en CI | ✅ Fase 6 |
| Persistencia fiel (BigDecimal, líneas, estado) | R02 | ✅ `JpaOrderRepositoryTest` (`@DataJpaTest`, H2) | ✅ Fase 3 |
| Concurrencia (bloqueo optimista, ADR-016) | R08 | ✅ `OptimisticLockingTest`: dos transacciones reales, la segunda falla. ✅ `ErrorHandlingTest.modificacionConcurrente` → 409 | ✅ Fase 3 |
| Casos de uso transaccionales | R08 | ✅ `RtbcOrdersApiApplicationTests.registraCasosDeUso` | ✅ Fase 3 |
| Observabilidad: logs de negocio | — | ✅ `BusinessEventLoggingTest`: creación (sin el cliente), cambio y rechazo con `codigo` | ✅ Fase 3 |
| Observabilidad: id de correlación `X-Request-Id` | R06 | ✅ `operacion.feature` (servidor real). ✅ `RequestIdFilterTest`: generación, reutilización, rechazo de valores inseguros (inyección en el log), presencia en el log, limpieza del MDC | ✅ Fase 3 |
| Contrato publicado sin desviaciones | R04 | ✅ `ApiDocumentationTest`: `/openapi.yaml` es byte a byte `docs/api/openapi.yaml`; sin especificación generada. Tipo `application/yaml` y Swagger UI: ✅ `operacion.feature` (servidor real) | ✅ Fase 4 |
| Pruebas de aceptación | R01-R05 | Karate: 30 escenarios en 6 features (4 obligatorios ⭐); prueba de la prueba en [estrategia-pruebas.md](estrategia-pruebas.md#9-pruebas-de-aceptación-con-karate) | ✅ Fase 4 |
| Artefacto Docker equivalente al local | R07 | `scripts/verificar-docker.ps1`: comandos del enunciado, contenedor `healthy`, sin root, logs JSON con `requestId` y Karate (30 escenarios) contra el contenedor con `docker compose` | ✅ Fase 5 (manual) y Fase 6: job `acceptance` en CI |
| Endpoint de salud sin detalles | R06 | ✅ `HealthEndpointTest`: `/actuator/health` UP sin componentes; `/actuator`, `env`, `beans`, `info` y `heapdump` → 404 | ✅ Fase 5 |
| Logs JSON en contenedor | — | ✅ `DockerProfileTest` (configuración) + `verificar-docker.ps1` (JSON con `requestId` real, validado en la Fase 5: 9/9 OK) | ✅ Fase 5 |
| Pruebas independientes del orden de ejecución | R09 | Job `build`: las pruebas se ejecutan también en orden inverso | ✅ Fase 6 |
| Análisis estático (code smells, bugs, seguridad) | R06, R09 | Job `sonar`: quality gate de SonarCloud | ✅ Fase 6 |
