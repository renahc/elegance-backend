# Stage 1: Compilación con Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copiar POMs para resolución del parent y dependencias
COPY pom.xml .
COPY elegance-user-service/pom.xml elegance-user-service/
COPY elegance-appointment-service/pom.xml elegance-appointment-service/
COPY elegance-notification-service/pom.xml elegance-notification-service/

# Copiar código fuente del microservicio
COPY elegance-user-service/src elegance-user-service/src

# Compilar empaquetando el JAR sin necesidad de precompilación manual
RUN mvn clean package -pl elegance-user-service -am -DskipTests

# Stage 2: Imagen ligera de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/elegance-user-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]