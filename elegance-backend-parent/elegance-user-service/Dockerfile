# elegance-user-service/Dockerfile
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiar el JAR construido
COPY target/*.jar app.jar

# Exponer el puerto
EXPOSE 8082

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]