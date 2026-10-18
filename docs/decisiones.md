# Registro de decisiones técnicas (ADR)

Cada decisión registra el contexto, la opción elegida y su consecuencia. Si una decisión cambia, se agrega una nueva entrada que la reemplaza; no se borra la anterior.

| ID | Decisión | Estado |
|---|---|---|
| [ADR-001](#adr-001-stack-tecnológico) | Stack: Java 17 + Spring Boot 3 + Maven Wrapper | Reemplazada parcialmente por ADR-015 (versión de Spring Boot) |
| [ADR-002](#adr-002-arquitectura-hexagonal) | Arquitectura hexagonal validada con ArchUnit | Aceptada |
| [ADR-003](#adr-003-persistencia-en-h2) | Persistencia en H2 vía JPA | Aceptada |
| [ADR-004](#adr-004-máquina-de-estados-en-el-dominio) | Máquina de estados centralizada en el dominio | Aceptada |
| [ADR-005](#adr-005-códigos-http) | Códigos HTTP 400 / 404 / 409 | Aceptada |
| [ADR-006](#adr-006-delete-como-cancelación-lógica) | DELETE como cancelación lógica | Aceptada |
| [ADR-007](#adr-007-total-calculado-en-el-servidor-con-bigdecimal) | Total calculado en servidor con `BigDecimal` | Aceptada |
| [ADR-008](#adr-008-validaciones-de-entrada) | Validaciones de entrada adicionales | Aceptada |
| [ADR-009](#adr-009-formato-de-error-único-rfc-9457) | Formato de error único (RFC 9457) | Aceptada |
| [ADR-010](#adr-010-get-orders-sin-paginación) | `GET /orders` sin paginación | Aceptada |
| [ADR-011](#adr-011-estrategia-de-pruebas-y-quality-gates) | Pirámide de pruebas y quality gates | Aceptada |
| [ADR-012](#adr-012-karate-contra-el-contenedor-en-ci) | Karate contra el contenedor en CI | Aceptada |
| [ADR-013](#adr-013-exclusiones-de-cobertura-y-mutación) | Exclusiones de cobertura y mutación | Aceptada |
| [ADR-014](#adr-014-versionamiento) | Conventional Commits, rama por fase, merge commit | Aceptada |
| [ADR-015](#adr-015-spring-boot-41) | Spring Boot 4.1 en lugar de 3.x | Aceptada |
| [ADR-016](#adr-016-bloqueo-optimista) | Bloqueo optimista para cambios de estado concurrentes | Aceptada |
| [ADR-017](#adr-017-alcance-de-spring-en-la-capa-de-aplicación) | Spring permitido en `application`, prohibido en `domain` | Aceptada |

---

## ADR-001: Stack tecnológico
- **Contexto:** el reto permite Java, Python o JavaScript/TypeScript.
- **Decisión:** Java 17, Spring Boot 3, Maven Wrapper, JUnit 5, Mockito, JaCoCo, PIT y Karate.
- **Consecuencia:** JaCoCo y PIT se integran con Maven y permiten fallar el build por umbral sin herramientas externas. Karate es nativo de la JVM, así que corre con el mismo build. Maven Wrapper fija la versión de Maven en local, CI y Docker.

## ADR-002: Arquitectura hexagonal
- **Contexto:** las reglas de negocio (RN-01 a RN-06) son el mayor riesgo y deben probarse aisladas, rápido y sin framework.
- **Decisión:** se usan cuatro capas:
  - `domain`: entidades, máquina de estados y excepciones de negocio. No depende de Spring.
  - `application`: un caso de uso por acción y los puertos (interfaces) de persistencia.
  - `infrastructure`: adaptador JPA/H2 que implementa los puertos.
  - `entrypoint`: controladores REST, DTOs y manejador global de errores.

  Las dependencias apuntan siempre hacia `domain`. ArchUnit lo verifica con una prueba que rompe el build si se viola.
- **Consecuencia:** el dominio se prueba con JUnit puro, en milisegundos y con alto mutation score. Cambiar H2 por otro motor solo toca `infrastructure`.

## ADR-003: Persistencia en H2
- **Contexto:** el reto pide ejecución local y en Docker sin dependencias externas.
- **Decisión:** se usa H2 en memoria vía Spring Data JPA, detrás de un puerto `OrderRepository`.
- **Consecuencia:** cero configuración para quien evalúa. Los datos se pierden al reiniciar, lo cual es aceptable para el alcance del reto. El adaptador se prueba con `@DataJpaTest`.

## ADR-004: Máquina de estados en el dominio
- **Contexto:** RN-04, RN-05 y RN-06 son reglas de transición. Si se dispersan en `if` dentro de controladores o servicios, se duplican y se escapan casos.
- **Decisión:** el enum `OrderStatus` define las transiciones permitidas en un solo lugar. Ver [maquina-estados.md](maquina-estados.md).
  - Flujo feliz: `CREADA → PAGADA → ENVIADA → ENTREGADA`.
  - Se puede cancelar desde `CREADA`, `PAGADA` y `ENVIADA`.
  - `ENTREGADA` y `CANCELADA` son estados finales.
  - Una transición al mismo estado actual es inválida.
- **Consecuencia:** la matriz completa de 25 combinaciones se prueba con una sola prueba parametrizada (RN-06).

## ADR-005: Códigos HTTP
- **Contexto:** el PDF fija 400 para enviar una orden no pagada y 409 para cancelar una orden entregada. Hay que extender el criterio al resto de casos de forma coherente.
- **Decisión:**

  | Situación | Código |
  |---|---|
  | Validación de entrada (body, JSON mal formado, estado desconocido, id con formato inválido) | 400 |
  | Enviar una orden en `CREADA` (RN-05) | 400 |
  | Orden inexistente | 404 |
  | Cualquier otra transición inválida, incluida cancelar `ENTREGADA` (RN-04), cancelar `CANCELADA` y pasar al mismo estado | 409 |

- **Consecuencia:** la regla para el cliente es simple. 400 significa que la petición no cumple una precondición de negocio o de formato. 409 significa que la petición entra en conflicto con el estado actual del recurso. RN-05 se trata como precondición (falta el pago), por eso devuelve 400 como pide el PDF.

## ADR-006: DELETE como cancelación lógica
- **Contexto:** el PDF nombra `DELETE /orders/{id}` como "Cancelar orden".
- **Decisión:** `DELETE` no borra el registro. Cambia el estado a `CANCELADA` usando el mismo caso de uso que `PATCH` con `{"estado": "CANCELADA"}` (DRY) y responde **200** con la orden actualizada.
- **Consecuencia:** se conserva el historial y la orden sigue consultable. Responder 200 con body, en lugar de 204, permite verificar el estado resultante en Karate y en E2E sin una segunda llamada.

## ADR-007: Total calculado en el servidor con BigDecimal
- **Contexto:** RN-03 pide calcular el total automáticamente. Usar `double` produce errores de redondeo en valores monetarios.
- **Decisión:** `total = Σ cantidad × valorUnitario`, calculado en el dominio con `BigDecimal`. Si el request trae `total` o `estado`, se ignoran: la orden siempre nace en `CREADA` con el total calculado.
- **Consecuencia:** el cliente no puede manipular el total. Las pruebas verifican que un `total` enviado a propósito con valor incorrecto sea ignorado.

## ADR-008: Validaciones de entrada
- **Contexto:** el PDF exige RN-01 (al menos un producto) y RN-02 (cantidad mayor que cero). Hay otros datos que, si llegan inválidos, corrompen el total o la orden.
- **Decisión:** además de RN-01 y RN-02, se exige:
  - `cliente` obligatorio y no vacío, máximo 120 caracteres.
  - `productos[].id` obligatorio y no vacío, máximo 50 caracteres.
  - `productos[].valorUnitario` mayor que 0, con máximo 2 decimales.
  - `productos[].cantidad` entero.

  Se permiten productos con el mismo `id` en una orden: se tratan como líneas independientes (YAGNI, el reto no define consolidación).
- **Consecuencia:** se evitan totales en cero o negativos y entradas sin límite de tamaño, que son un riesgo de seguridad. Cada validación tiene pruebas de límite.

## ADR-009: Formato de error único (RFC 9457)
- **Contexto:** los errores deben ser consistentes y fáciles de verificar en pruebas.
- **Decisión:** todas las respuestas de error usan `application/problem+json` (`ProblemDetail`, nativo de Spring 6), con dos campos adicionales:
  - `codigo`: identificador estable del error, por ejemplo `ORDEN_NO_PAGADA`.
  - `errores`: lista de errores de campo, solo en errores de validación.

  Un único `@RestControllerAdvice` hace el mapeo.
- **Consecuencia:** las pruebas verifican `codigo` en lugar de mensajes de texto, que son frágiles. No se exponen stack traces ni detalles internos.

## ADR-010: GET /orders sin paginación
- **Contexto:** el PDF no pide paginación ni filtros.
- **Decisión:** `GET /orders` devuelve la lista completa (YAGNI).
- **Consecuencia:** se acepta como deuda técnica explícita. Si el volumen creciera, se agregaría paginación sin romper el contrato, envolviendo la lista en un objeto con versión nueva del endpoint.

## ADR-011: Estrategia de pruebas y quality gates
- **Contexto:** el reto valora la estrategia basada en riesgo y la pirámide de pruebas.
- **Decisión:** se aplica una pirámide con más pruebas abajo (dominio) y pocas arriba (Karate y E2E). Los quality gates son:
  - Cobertura de líneas y de ramas mayor o igual a 80% (JaCoCo).
  - Mutation score mayor o igual a 70% (PIT).
  - Cero pruebas fallidas.

  El build falla si no se cumple alguno. Detalle en [qa/estrategia-pruebas.md](qa/estrategia-pruebas.md).
- **Consecuencia:** las métricas son una señal y no el objetivo. Los mutantes que sobreviven se analizan y se documentan.

## ADR-012: Karate contra el contenedor en CI
- **Contexto:** Karate puede correr contra la aplicación levantada por Maven o contra la imagen Docker.
- **Decisión:** en local, Karate apunta a una URL base configurable. En CI corre contra el contenedor construido en el mismo pipeline.
- **Consecuencia:** las pruebas de aceptación validan el mismo artefacto que se entrega. Así detectan también errores de empaquetado o configuración.

## ADR-013: Exclusiones de cobertura y mutación
- **Contexto:** excluir código para subir métricas sería maquillaje. No excluir nada penaliza código sin lógica.
- **Decisión:** solo se excluyen la clase `main` de Spring Boot y las clases de configuración sin lógica condicional. Los DTOs y el manejador de errores **no** se excluyen. PIT muta `domain` y `application`, que es donde vive la lógica de negocio. Cada exclusión se lista en el `pom.xml` con un comentario que la justifica.
- **Consecuencia:** quien evalúa puede revisar cada exclusión y su motivo.

## ADR-014: Versionamiento
- **Decisión:** se usan Conventional Commits (tipo en inglés, descripción en español), una rama por fase y PR con **merge commit**, sin squash. El merge commit conserva la secuencia TDD `test → feat → refactor` como evidencia. Cada fase cierra con un tag semántico. Detalle en [CONTRIBUTING.md](../CONTRIBUTING.md).

## ADR-015: Spring Boot 4.1
- **Contexto:** ADR-001 fijaba Spring Boot 3. Al generar el proyecto (2026-10-18), la línea 3.x ya no está disponible en start.spring.io y el soporte OSS de 3.5 terminó el 2026-06-30. Fuente: calendario oficial de soporte de spring.io.
- **Decisión:** Spring Boot **4.1.1**, la versión estable vigente, con soporte OSS hasta el 2027-07-31. Sigue compatible con Java 17.
- **Consecuencia:**
  - Spring Boot 4 usa Spring Framework 7. `ProblemDetail` (ADR-009) sigue disponible.
  - Los starters son modulares: `spring-boot-starter-webmvc` y `spring-boot-starter-webmvc-test`.
  - Spring Boot 4 usa Jackson 3 (paquete `tools.jackson`). En la Fase 4 se verificará que Karate, que usa Jackson 2, conviva sin conflictos en el classpath de pruebas.

## ADR-016: Bloqueo optimista
- **Contexto:** riesgo R08. Dos cambios de estado simultáneos sobre la misma orden podrían saltarse la máquina de estados, por ejemplo cancelar y entregar al mismo tiempo.
- **Decisión:** se agrega un campo `@Version` en la entidad JPA. Si hay conflicto de versión, la API responde 409 `TRANSICION_INVALIDA`.
- **Consecuencia:** el costo es mínimo (una columna) y no requiere bloqueos en la base de datos. Se prueba con `@DataJpaTest` en la Fase 3.

## ADR-017: Alcance de Spring en la capa de aplicación
- **Contexto:** ADR-002 prohíbe que `domain` dependa de frameworks. Hay que decidir si `application` también debe estar libre de Spring.
- **Decisión:** `domain` solo puede depender de `java..`, y ArchUnit lo verifica. `application` puede usar anotaciones de Spring (`@Service`, `@Transactional`). Así se evita una capa extra de configuración solo para registrar los beans (KISS).
- **Consecuencia:** las reglas de negocio siguen aisladas y probables sin framework. Los casos de uso se prueban con Mockito, sin levantar el contexto de Spring.
