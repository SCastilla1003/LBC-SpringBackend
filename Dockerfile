# syntax=docker/dockerfile:1
# =====================================================================
# LBC API - imagen para Railway (o cualquier servidor con Docker).
# Etapa 1: compila con Maven + JDK 17. Etapa 2: solo el JRE 17 y el JAR.
# =====================================================================

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: las dependencias quedan en caché mientras no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests se ejecutan en local (./mvnw test); aquí solo se empaqueta
RUN mvn -B -q package -DskipTests


FROM eclipse-temurin:17-jre
WORKDIR /app

# No ejecutar como root
RUN useradd --system --uid 1001 --no-create-home lbc
COPY --from=build /app/target/*.jar /app/app.jar
USER lbc

# La JVM usa como máximo el 75 % de la memoria del contenedor y se reinicia si se queda sin memoria
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Railway inyecta PORT; la app escucha en ${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
