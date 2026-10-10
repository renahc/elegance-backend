# Élégance Beauty Studio - Backend Microservices Architecture

Backend desacoplado en microservicios desarrollado con **Java 17**, **Spring Boot 3.2.3**, **Azure AD (Microsoft Entra ID)**, **Docker**, **Terraform** y despliegue continuo en **AWS EC2** con **GitHub Actions**.

---

## 🏛️ Arquitectura del Sistema

El proyecto está organizado como un repositorio multi-módulo Maven (`elegance-backend-parent`), compuesto por tres microservicios independientes:

```
elegance-backend/
├── .github/
│   └── workflows/
│       └── deploy-backend.yml         # Pipeline CI/CD automatizado hacia AWS EC2
├── docker-compose.yml                 # Orquestación de desarrollo local
├── elegancebd.sql                     # Script DDL y de seed para MySQL/MariaDB
├── infra/
│   └── terraform/                     # Infraestructura como Código (IaC) para AWS EC2
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── scripts/
│           └── restart.sh             # Script de arranque y reinicio de servicios
├── scripts/
│   └── restart.sh                     # Script de arranque en la máquina host/EC2
└── elegance-backend-parent/           # Proyecto Maven Multi-Módulo
    ├── pom.xml                        # POM Padre con gestión de dependencias y versiones
    ├── elegance-user-service/         # Microservicio de Usuarios y Personal (Puerto 8082)
    ├── elegance-appointment-service/  # Microservicio de Citas y Reservas (Puerto 8081)
    └── elegance-notification-service/ # Microservicio de Notificaciones por Email (Puerto 8083)
```

---

## 📦 Microservicios

### 1. `elegance-user-service` (Puerto `8082`)
- **Base de Datos:** MySQL / MariaDB (`elegance_users`)
- **Responsabilidad:**
  - **Clientes (CRM):** Listar clientes, búsqueda por nombre, registrar, editar datos de perfil y fidelización (`tier`), y eliminar (`/api/v1/clients`).
  - **Estilistas / Staff:** Listar equipo, registrar estilistas, actualizar especialidades, calificaciones y cambiar disponibilidad en tiempo real (`/api/v1/stylists`).
- **Documentación Swagger:** `http://localhost:8082/swagger-ui.html`

### 2. `elegance-appointment-service` (Puerto `8081`)
- **Base de Datos:** MySQL / MariaDB (`elegance_appointments`)
- **Responsabilidad:**
  - **Agenda y Reservaciones:** Crear citas, consultar agenda con filtros por fecha y estado (`confirmada`, `en_proceso`, `cancelada`), actualizar estado y cancelar reservas (`/api/v1/appointments`).
  - **Autonomía de Datos:** Mantiene una instantánea (*snapshot*) desnormalizada de los datos del cliente, estilista y servicio en `AppointmentEntity` para evitar acoplamiento síncrono con `user-service`.
- **Documentación Swagger:** `http://localhost:8081/swagger-ui.html`

### 3. `elegance-notification-service` (Puerto `8083`)
- **Responsabilidad:**
  - **Notificaciones Transaccionales:** Envío asíncrono de correos electrónicos mediante **Spring Mail** y protocolo SMTP (`/api/v1/notifications/email`).
- **Documentación Swagger:** `http://localhost:8083/swagger-ui.html`

---

## 🔐 Seguridad con Azure AD / Microsoft Entra ID

Cada microservicio opera de forma autónoma como un **OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`):
- Validación de tokens JWT en la cabecera `Authorization: Bearer <TOKEN_AZURE>`.
- Comprobación del emisor (*Issuer*) y validación de la audiencia (*Audience / Client ID*).
- **Configuración Dinámica:** Los valores de emisor y audiencia son parametrizables vía variables de entorno o GitHub Secrets, contando con valores por defecto de contingencia:
  - `AZURE_AD_ISSUER_URI` (por defecto: `https://sts.windows.net/ff064edc-07f4-448c-97e1-49da14c085f5/`)
  - `AZURE_AD_CLIENT_ID` (por defecto: `api://304d54f7-d485-478a-a1ea-0c2f874b0c1f`)
- Rutas públicas exentas de autenticación:
  - `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/actuator/health`

---

## 🚀 Ejecución en Desarrollo Local

### Opción A: Con Docker Compose (Recomendado)
Los microservicios cuentan con **Multi-stage Dockerfiles** que compilan el código fuente dentro del contenedor automáticamente mediante Maven, por lo que **no requieres tener Maven o Java instalados localmente**.

```bash
# Construir imágenes y levantar servicios junto con MySQL
docker compose up --build
```

Servicios disponibles:
- **MySQL 8.0:** `localhost:3306` (Base de datos: `elegance_db`, Usuario: `root`, Password: `rootpassword`)
- **User Service:** `http://localhost:8082`
- **Appointment Service:** `http://localhost:8081`
- **Notification Service:** `http://localhost:8083`

---

### Opción B: Ejecución Manual con Maven Wrapper

1. **Requisitos:** Java 17 instalado.
2. **Compilar todos los módulos desde la raíz del proyecto parent:**
   ```bash
   cd elegance-backend-parent
   # En Windows:
   .\mvnw.cmd clean package -DskipTests
   # En Linux / macOS:
   ./mvnw clean package -DskipTests
   ```
3. **Ejecutar cada microservicio de forma independiente:**
   ```bash
   # Terminal 1 - Users:
   java -jar elegance-user-service/target/elegance-user-service-1.0.0-SNAPSHOT.jar

   # Terminal 2 - Appointments:
   java -jar elegance-appointment-service/target/elegance-appointment-service-1.0.0-SNAPSHOT.jar

   # Terminal 3 - Notifications:
   java -jar elegance-notification-service/target/elegance-notification-service-1.0.0-SNAPSHOT.jar
   ```

---

## ☁️ Infraestructura en AWS (Terraform) & CI/CD

### Infraestructura como Código (`infra/terraform`)
- **Recursos provisionados:**
  - Instancia **AWS EC2 `t3.small`** con **Amazon Linux 2023** en `us-east-1`.
  - Configuración automatizada con `user_data`: Instalación de Java 17 Corretto, MariaDB 10.5 (`mariadb105-server`), aprovisionamiento de bases de datos (`elegance_users` y `elegance_appointments`) y directorio `/opt/elegance`.
  - Security Group con apertura restringida a SSH (puerto 22) y puertos de microservicios (`8081`, `8082`, `8083`).
  - Generación dinámica de par de llaves SSH (`tls_private_key`).
  - Backend remoto de estado en **AWS S3** (`elegance-tf-state-2026-gabriel`) con bloqueo de concurrencia en **DynamoDB** (`elegance-tf-lock`).

### Pipeline CI/CD (`deploy-backend.yml`)
El flujo de GitHub Actions se ejecuta automáticamente en cada `push` o `pull_request` a la rama `Develop/Microservicios`:
1. Realiza el checkout del repositorio e instala JDK 17 (Temurin).
2. Compila y empaqueta los tres JARs con Maven Wrapper.
3. Autentica en AWS y ejecuta `terraform apply -auto-approve`.
4. Espera a que la instancia EC2 y el motor de base de datos estén listos.
5. Genera el archivo `/opt/elegance/.env` con las credenciales seguras.
6. Transfiere vía SCP los tres artefactos `.jar`, el archivo `.env` y el script `restart.sh`.
7. Ejecuta `restart.sh` vía SSH para arrancar los servicios en segundo plano con `nohup`.

### Secretos Requeridos en GitHub Actions

| Nombre del Secreto | Descripción |
| :--- | :--- |
| `AWS_ACCESS_KEY_ID` | Clave de acceso IAM de AWS para Terraform |
| `AWS_SECRET_ACCESS_KEY` | Clave secreta IAM de AWS |
| `AWS_SESSION_TOKEN` | Token de sesión temporal (si se utiliza AWS Academy / Learner Lab) |
| `TEST_DB_PASSWORD` | Contraseña asignada al usuario `root` de la base de datos MySQL en EC2 |
| `MAIL_USERNAME` | Correo electrónico de salida SMTP (Gmail) |
| `MAIL_PASSWORD` | Contraseña de aplicación para autenticación SMTP |
| `AZURE_AD_ISSUER_URI` | *(Opcional)* URI del emisor de tokens Azure AD |
| `AZURE_AD_CLIENT_ID` | *(Opcional)* Client ID o Audiencia (`aud`) de Azure AD |
