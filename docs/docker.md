# Ejecución con Docker

Requisitos: Docker en ejecución. No hace falta Java para construir, ejecutar ni probar la imagen.

## Comandos del enunciado

```bash
docker build -t challenge-api .
docker run -p 8080:8080 challenge-api
```

- API: `http://localhost:8080/orders`
- Contrato (Swagger UI): `http://localhost:8080/swagger-ui.html`
- Salud: `http://localhost:8080/actuator/health`

## Con Docker Compose

| Comando | Qué hace |
|---|---|
| `docker compose up --build -d --wait` | Construye la imagen, levanta la API y espera a que esté `healthy` |
| `docker compose logs -f api` | Muestra los logs en JSON (cada línea incluye `requestId`) |
| `docker compose --profile acceptance run --rm karate` | Ejecuta los 30 escenarios de Karate contra el contenedor, dentro de otro contenedor |
| `docker compose --profile acceptance down` | Detiene y elimina los contenedores |

El reporte de Karate queda en `target/karate-reports/karate-summary.html`.

Si tienes Java 17 instalado, también puedes correr Karate desde tu máquina contra el contenedor publicado en el puerto 8080:

```bash
./mvnw verify -Pacceptance -DskipUnitTests=true -Dacceptance.skipAppStart=true -Dkarate.env=docker
```

## Verificación completa (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-docker.ps1
```

El script ejecuta los comandos del enunciado y verifica:
- que el contenedor quede `healthy`;
- que `/actuator/health` responda;
- que el contenedor no corra como root;
- que los logs salgan en JSON con el `requestId`;
- el tamaño de la imagen;
- Karate contra el contenedor.

Deja un resumen en `target/verificacion-docker.txt`.

## Cómo está construida la imagen

Ver [ADR-021](decisiones.md#adr-021-imagen-docker).

| Aspecto | Decisión |
|---|---|
| Etapas | `build` (JDK 17 + Maven Wrapper) y una imagen final que solo tiene el JRE 17 y la aplicación |
| Base | `eclipse-temurin:17.0.20_8-jre-noble`, con versión fija para que los builds sean reproducibles |
| Capas | La aplicación se separa en capas de Spring Boot (dependencias, loader, aplicación). Un cambio de código solo reconstruye la última |
| Usuario | `10001:10001`, sin privilegios ni shell de login |
| Perfil | `docker`: logs JSON (ECS) con `requestId` |
| JVM | `-XX:MaxRAMPercentage=75` (heap según la memoria del contenedor) y `-XX:+ExitOnOutOfMemoryError` |
| Salud | `HEALTHCHECK` sobre `/actuator/health`, el único endpoint de actuator expuesto |
| Contexto | `.dockerignore` en modo lista blanca: solo `.mvn/`, `mvnw`, `pom.xml`, `src/main/` y `docs/api/openapi.yaml` |
| Pruebas | No se ejecutan en `docker build`: el pipeline las corre antes de construir la imagen |

## Evidencia de ejecución

Script `scripts/verificar-docker.ps1` ejecutado en Windows con Docker Desktop, sobre el commit `50cbd0c` (2026-10-19):

| Verificación | Resultado | Detalle |
|---|---|---|
| `docker build -t challenge-api .` | OK | exit 0 |
| `docker run -p 8080:8080 challenge-api` | OK | exit 0 |
| HEALTHCHECK del contenedor | OK | estado: healthy |
| `GET /actuator/health` | OK | status: UP |
| Contenedor sin usuario root | OK | uid: 10001 |
| `POST /orders` | OK | HTTP 201, total 200000 |
| Log JSON con `requestId` | OK | requestId en el log: verificacion-docker-1 |
| Tamaño de la imagen | OK | 480 MB (sin comprimir) |
| Karate contra el contenedor | OK | scenarios: 30, passed: 30, failed: 0 |

**Posible mejora:** la imagen pesa 480 MB sin comprimir. La mayor parte es la base JRE de Ubuntu (unos 270 MB). Una base Alpine la reduciría, pero se descartó por compatibilidad (ADR-021). El enunciado no pone límite de tamaño.
