# ==========================================================================
# Backend EduckTrack - build multi-stage (RS-10: contenedores independientes)
# ==========================================================================

# --- Etapa 1: build con Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache de dependencias: primero el pom, luego el codigo
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Etapa 2: runtime ligero ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Usuario no root por seguridad
RUN addgroup -S educk && adduser -S educk -G educk
COPY --from=build /app/target/educktrack-backend.jar app.jar
USER educk
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]
