# Élégance Beauty Studio - Spring Boot Backend & Azure AD Security

Servicio Backend en **Spring Boot 3.2.3 (Java 17)** desarrollado para la evaluación parcial N° 1 de Desarrollo Cloud Native I (Duoc UC).

## Características Principales

1. **Seguridad Azure AD / IDaaS (MSAL / Microsoft Entra ID)**:
   - Configuración OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`).
   - Validación de tokens JWT en cabeceras `Authorization: Bearer <TOKEN_AZURE>`.
   - Control de acceso por roles y verificación del emisor (issuer) y audiencia (clientId).
   - Extracción de atributos de usuario e identidad en `/api/v1/auth/me`.

2. **Persistencia JPA & Base de Datos Dual**:
   - Mapeo de Entidades: `ServiceEntity`, `StylistEntity`, `ClientEntity`, `AppointmentEntity`, `NotificationEntity`.
   - Base de datos local ligera H2 (`jdbc:h2:mem:salondb`) con consola web habilitada en `/h2-console`.
   - Sedeo automático de datos iniciales en arranque (`DataSeeder.java`).
   - Perfil de producción listo para **Azure Database for PostgreSQL** o Azure SQL.

3. **Endpoints REST API**:
   - `/api/v1/appointments` (GET, POST, PUT status, DELETE)
   - `/api/v1/services` (GET, POST, PUT toggle)
   - `/api/v1/stylists` (GET, PUT availability)
   - `/api/v1/clients` (GET, POST)
   - `/api/v1/analytics/kpis` (GET)
   - `/api/v1/auth/me` (GET user info desde JWT Azure AD)

4. **Documentación Interactiva OpenAPI / Swagger UI**:
   - Disponible al iniciar el servidor en: `http://localhost:8080/swagger-ui.html`

---

## Cómo Ejecutar Localmente

```bash
# 1. Navegar al directorio del backend
cd /Users/renatoherrera/Documents/workspace/duoc/elegance/salon-backend

# 2. Instalar JDK y Maven si no están instalados
brew install openjdk maven

# 3. Compilar e iniciar el servidor Spring Boot
mvn spring-boot:run
```

El servidor iniciará en `http://localhost:8080`.

---

## Estructura del Proyecto

```
salon-backend/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/com/elegance/salon/
        │   ├── SalonBackendApplication.java
        │   ├── config/
        │   │   ├── SecurityConfig.java
        │   │   └── DataSeeder.java
        │   ├── controller/
        │   │   ├── AppointmentController.java
        │   │   ├── ServiceController.java
        │   │   ├── StylistController.java
        │   │   ├── ClientController.java
        │   │   ├── AnalyticsController.java
        │   │   └── AuthController.java
        │   ├── dto/
        │   │   ├── AppointmentRequest.java
        │   │   ├── KpiResponse.java
        │   │   └── UserInfoDto.java
        │   ├── model/
        │   │   ├── AppointmentEntity.java
        │   │   ├── ServiceEntity.java
        │   │   ├── StylistEntity.java
        │   │   ├── ClientEntity.java
        │   │   └── NotificationEntity.java
        │   └── repository/
        │       ├── AppointmentRepository.java
        │       ├── ServiceRepository.java
        │       ├── StylistRepository.java
        │       ├── ClientRepository.java
        │       └── NotificationRepository.java
        └── resources/
            └── application.yml
```
