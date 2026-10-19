# syntax=docker/dockerfile:1
# Imagen de la API de órdenes (ADR-021).
#   docker build -t challenge-api .
#   docker run -p 8080:8080 challenge-api

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:17.0.20_8-jdk-noble AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY docs/api/openapi.yaml docs/api/openapi.yaml
COPY src/main/ src/main/

# Las pruebas no se ejecutan aquí: el pipeline las corre antes de construir la imagen (ADR-021).
# El caché de ~/.m2 acelera las reconstrucciones sin guardar las dependencias en la imagen.
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -ntp package -Dmaven.test.skip=true \
 && java -Djarmode=tools -jar target/rtbc-orders-api-*.jar extract --layers --launcher --destination target/extracted

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17.0.20_8-jre-noble

# Usuario sin privilegios con UID fijo (no root)
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

# Capas de Spring Boot de la menos a la más cambiante: una reconstrucción solo reemplaza la capa de la aplicación
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

# UID numérico: verificable por el orquestador (p. ej. runAsNonRoot en Kubernetes)
USER 10001:10001

# Perfil docker: logs JSON. La JVM dimensiona el heap según la memoria del contenedor.
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health || exit 1"]

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
