# Matriz de trazabilidad: requisitos → riesgos → pruebas

Conecta cada requisito del reto con su riesgo y con las pruebas que lo cubren. Se actualiza al cerrar cada fase: la columna **Estado** pasa de 📝 Planificado a ✅ Implementado con el nombre real de la prueba.

**Convención de etiquetas** (permite filtrar la ejecución por regla):
- JUnit: `@Tag("RN-01")` → `./mvnw test -Dgroups=RN-01`
- Karate: `@RN-01` → `-Dkarate.options="--tags @RN-01"`

## Reglas de negocio

| Req. | Descripción | Riesgo | Unitarias (dominio / caso de uso) | Integración (`@WebMvcTest`) | Karate | E2E | Estado |
|---|---|---|---|---|---|---|---|
| RN-01 | La orden debe tener al menos un producto | R03 | Rechaza una lista vacía y una lista nula. Acepta un producto | POST sin productos → 400 `VALIDACION` | `@RN-01` crear orden sin productos → 400 | — | 📝 Fase 2-4 |
| RN-02 | La cantidad debe ser mayor que 0 | R03 | Límites −1 y 0 se rechazan, 1 se acepta | POST con cantidad 0 → 400 | `@RN-02` cantidad 0 → 400 | — | 📝 Fase 2-4 |
| RN-03 | Total calculado automáticamente | R02 | Una línea, varias líneas, decimales, valores grandes | — | `@RN-03` el total enviado se ignora y se recalcula | Crear orden muestra el total | 📝 Fase 2-4, 8b |
| RN-04 | Una orden entregada no se puede cancelar | R01 | `ENTREGADA → CANCELADA` lanza transición inválida | DELETE de una orden entregada → 409 | **`@RN-04` obligatorio:** cancelar una orden entregada → 409 | — | 📝 Fase 2-4 |
| RN-05 | No se puede enviar sin pagar | R01 | `CREADA → ENVIADA` lanza orden no pagada | PATCH ENVIADA sobre una orden creada → 400 | **`@RN-05` obligatorio:** enviar una orden no pagada → 400 | — | 📝 Fase 2-4 |
| RN-06 | No se permiten transiciones inválidas | R01, R08 | Prueba parametrizada de las 25 combinaciones (ver [maquina-estados.md](../maquina-estados.md)) | PATCH inválido → 409 `TRANSICION_INVALIDA` | `@RN-06` `Scenario Outline` con una muestra por código HTTP y el flujo completo | Actualizar estado | 📝 Fase 2-4, 8b |

## Endpoints y escenarios obligatorios del PDF

| Endpoint | Escenario | Código | Riesgo | Karate | Integración | E2E | Estado |
|---|---|---|---|---|---|---|---|
| `POST /orders` | **Crear una orden válida** (obligatorio) | 201 + `Location` | R02, R03 | `@crear` | ✓ | Crear orden | 📝 Fase 3-4 |
| `GET /orders/{id}` | **Consultar una orden existente** (obligatorio) | 200 | R04 | `@consultar` | ✓ | Consultar orden | 📝 Fase 3-4 |
| `GET /orders/{id}` | Orden inexistente | 404 | R05 | `@consultar` | ✓ | — | 📝 Fase 3-4 |
| `GET /orders/{id}` | UUID mal formado | 400 | R05 | — | ✓ | — | 📝 Fase 3 |
| `GET /orders` | Listar órdenes | 200 | R04 | `@listar` | ✓ | — | 📝 Fase 3-4 |
| `PATCH /orders/{id}/status` | Estado desconocido o ausente | 400 | R05 | `@RN-06` | ✓ | — | 📝 Fase 3-4 |
| `DELETE /orders/{id}` | Cancelar una orden creada | 200 | R01 | `@cancelar` | ✓ | Cancelar orden | 📝 Fase 3-4 |

## Validaciones adicionales (ADR-008)

| Validación | Riesgo | Unitarias / Integración | Karate | Estado |
|---|---|---|---|---|
| `cliente` obligatorio, entre 1 y 120 caracteres | R03 | Límites 0, 1, 120 y 121 | `@validaciones` cliente vacío → 400 | 📝 Fase 2-4 |
| `productos[].id` obligatorio, entre 1 y 50 caracteres | R03 | Límites 0, 1, 50 y 51 | — | 📝 Fase 2-3 |
| `valorUnitario` > 0 con máximo 2 decimales | R02, R03 | 0, 0.01 y 0.001 | `@validaciones` valor 0 → 400 | 📝 Fase 2-4 |
| JSON mal formado | R05 | `@WebMvcTest` → 400 | — | 📝 Fase 3 |
| Sin fuga de información en errores | R06 | `@WebMvcTest` verifica que no haya `trace` ni `exception` | — | 📝 Fase 3 |

## Requisitos no funcionales

| Requisito | Riesgo | Verificación | Estado |
|---|---|---|---|
| Dependencias entre capas | R10 | ArchUnit: `ArchitectureTest` (2 reglas) | ✅ Fase 1 |
| Cobertura ≥ 80% de líneas y ramas | R09 | JaCoCo `check` en el build y en CI | 📝 Fase 2, 6 |
| Mutation score ≥ 70% | R09 | PIT `mutationThreshold` en el build y en CI | 📝 Fase 2, 6 |
| Artefacto Docker equivalente al local | R07 | Karate contra el contenedor en CI | 📝 Fase 5-6 |
| Concurrencia (bloqueo optimista, ADR-016) | R08 | `@DataJpaTest` | 📝 Fase 3 |
