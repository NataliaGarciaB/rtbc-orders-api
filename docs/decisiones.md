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
| [ADR-018](#adr-018-swagger-ui-con-el-contrato-de-diseño) | Swagger UI muestra el contrato de diseño, sin especificación generada | Aceptada |
| [ADR-019](#adr-019-observabilidad-logs-de-negocio-e-id-de-correlación) | Logs de eventos de negocio e id de correlación `X-Request-Id` | Aceptada |
| [ADR-020](#adr-020-reglas-estrictas-de-json) | Reglas estrictas de lectura y escritura de JSON | Aceptada |
| [ADR-021](#adr-021-imagen-docker) | Imagen Docker multi-stage, sin root, con healthcheck y Karate en contenedor | Aceptada |
| [ADR-022](#adr-022-pipeline-de-ci-y-quality-gates) | Pipeline de CI con jobs paralelos, quality gates y publicación en GHCR | Aceptada |

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

## ADR-018: Swagger UI con el contrato de diseño
- **Contexto:** el contrato `docs/api/openapi.yaml` se diseñó y revisó en la Fase 0. Una especificación generada desde el código sería una segunda fuente que puede contradecirlo. springdoc solo muestra Swagger UI si además publica la especificación generada.
- **Decisión:** no se usa springdoc. Maven copia `docs/api/openapi.yaml` a los recursos estáticos durante el build, y `/swagger-ui.html` usa el webjar de Swagger UI para mostrarlo. `/v3/api-docs` no existe.
- **Consecuencia:**
  - Hay una sola fuente de verdad y el código no tiene anotaciones de documentación.
  - Las pruebas `@WebMvcTest` y Karate verifican que la implementación cumple el contrato.
  - `/openapi.yaml` se sirve como `application/yaml` en el servidor real (`server.mime-mappings`). MockMvc no aplica esa configuración, así que el tipo se verifica en Karate, contra el servidor real.

## ADR-019: Observabilidad: logs de negocio e id de correlación
- **Contexto:** el reto valora la observabilidad. Hace falta poder explicar qué le pasó a una orden y seguir una petición en los logs.
- **Decisión:**
  - Los casos de uso registran en formato `clave=valor`:
    - `Orden creada` y `Estado cambiado`, en nivel INFO.
    - `Transición rechazada`, en nivel WARN, con su `codigo`.
  - Los logs no incluyen el nombre del cliente (dato personal).
  - Un filtro asigna un `X-Request-Id` a cada petición. Si el cliente envía uno seguro (`[A-Za-z0-9._-]{1,64}`), se reutiliza; si no, se genera un UUID. Así se evita la inyección de líneas en el log.
  - El id se publica en el MDC y en la respuesta.
  - Los errores inesperados se registran completos en el log y nunca en la respuesta (R06).
- **Consecuencia:** cada línea de log de una petición lleva `[requestId]`. Las pruebas verifican los mensajes, así que PIT no puede eliminar una llamada a log sin que una prueba falle. El formato JSON para Docker se configura en la Fase 5.

## ADR-020: Reglas estrictas de JSON
- **Contexto:** Jackson 3 tiene comportamientos por defecto que son riesgosos para un sistema de órdenes. Durante la Fase 3 se detectaron dos con pruebas:
  - trunca `"cantidad": 1.5` a `1` sin avisar;
  - escribe `BigDecimal` con escala negativa en notación científica (`1E+5`).
- **Decisión:** `spring.jackson.deserialization.accept-float-as-int=false` (una cantidad decimal responde 400) y `spring.jackson.write.write-bigdecimal-as-plain=true`. Además, `valorUnitario` admite como máximo 17 enteros y 2 decimales (`@Digits`), para que coincida con la columna `unit_price` (precision 19, scale 2). Sin ese límite, un valor enorme provocaría un 500 en la base de datos.
- **Consecuencia:** cada regla tiene una prueba que falla si se desactiva.

## ADR-021: Imagen Docker
- **Contexto:** el reto pide `Dockerfile` y `docker-compose.yml`, y que la aplicación se construya y ejecute con `docker build` y `docker run`. Karate debe validar el mismo artefacto que se entrega (ADR-012, riesgo R07).
- **Decisión:**
  - **Multi-stage:** se compila con `eclipse-temurin:17.0.20_8-jdk-noble` y se ejecuta con `eclipse-temurin:17.0.20_8-jre-noble`. Las versiones están fijadas, y la base Ubuntu se eligió por compatibilidad.
  - **Capas:** el JAR se extrae en capas de Spring Boot (`jarmode=tools extract --layers --launcher`), así una reconstrucción solo cambia la capa de la aplicación.
  - **Seguridad:** la imagen corre con el usuario `10001:10001`, sin privilegios. El `.dockerignore` funciona como lista blanca (solo entra lo que la compilación necesita) y la imagen no contiene secretos.
  - **Sin pruebas en `docker build`:** el pipeline las ejecuta antes de construir la imagen, y repetirlas costaría unos 2 minutos por build.
  - **Salud:** se expone solo `/actuator/health`, sin detalles ni índice de endpoints, para el `HEALTHCHECK` y para que el pipeline sepa cuándo el contenedor está listo.
  - **Perfil `docker`:** logs JSON (ECS) con `requestId` como campo propio, para que una herramienta de logs los procese.
  - **JVM:** `-XX:MaxRAMPercentage=75` y `-XX:+ExitOnOutOfMemoryError`. En compose, el límite es de 512 MB.
  - **Karate en un contenedor del compose** (perfil `acceptance`): basta con Docker para correr las pruebas de aceptación contra la imagen.
- **Consecuencia:** cualquier persona con Docker puede construir, ejecutar y validar la API sin instalar Java. El script `scripts/verificar-docker.ps1` comprueba todo lo anterior de punta a punta. Detalle de uso en [docker.md](docker.md).

## ADR-022: Pipeline de CI y quality gates
- **Contexto:** el reto pide un pipeline con checkout, build, pruebas unitarias, cobertura, mutation testing, Karate e imagen Docker, que falle si la cobertura baja de 80 %, el mutation score baja de 70 % o alguna prueba falla. SonarCloud es opcional.
- **Decisión:** GitHub Actions con cinco jobs:

  | Job | Qué hace | Falla si |
  |---|---|---|
  | `build` | `./mvnw verify` y luego las mismas pruebas en orden inverso | Alguna prueba falla, la cobertura es menor que 80 % o hay dependencias de orden entre pruebas |
  | `mutation` | PIT con mutadores STRONGER sobre `domain` y `application` | El mutation score es menor que 70 % |
  | `acceptance` | Construye la imagen, la levanta con `docker compose up --wait` y ejecuta Karate contra el contenedor (ADR-012) | Algún escenario falla o el contenedor no llega a `healthy` |
  | `sonar` | Análisis de SonarCloud con la cobertura de JaCoCo | El quality gate de SonarCloud falla |
  | `publish` | Publica la imagen en `ghcr.io/nataliagarciab/rtbc-orders-api`, con etiquetas `sha`, `latest` y versión semántica | — (solo corre en push a `main` y en tags `v*`, si los otros cuatro pasaron) |

  - `mutation`, `acceptance` y `sonar` corren en paralelo después de `build`.
  - Cada job publica sus reportes como artifacts (JaCoCo, Surefire, PIT, Karate y los logs del contenedor) y un resumen con las métricas en la ejecución.
  - Todas las actions se fijan por SHA de commit, con la versión en un comentario. Una etiqueta como `@v7` se puede mover: si el repositorio de la action fuera comprometido, código ajeno correría en el pipeline con permiso para publicar la imagen. SonarCloud lo detectó en el primer análisis (regla S7637).
  - Los permisos son mínimos (`contents: read`). Solo `publish` tiene `packages: write`, y `sonar` no corre en PR desde forks, porque no reciben secretos.
- **Consecuencia:** `build`, `mutation`, `acceptance` y `sonar` son checks obligatorios en el ruleset de `main`. Ningún cambio llega a `main` sin pasar los quality gates, y la imagen publicada es la misma que validó Karate. La Fase 8b consume esa imagen desde GHCR.
