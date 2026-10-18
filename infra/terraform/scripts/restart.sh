#!/bin/bash
set -e

echo "🔄 Iniciando reinicio de servicios..."

# 1. Cargar variables de entorno desde el .env generado por GitHub Actions
if [ -f /opt/elegance/.env ]; then
    echo "✅ Cargando variables desde /opt/elegance/.env"
    set -a
    source /opt/elegance/.env
    set +a
else
    echo "❌ ERROR FATAL: No se encontró /opt/elegance/.env"
    exit 1
fi

# 2. Detener procesos anteriores
echo "🛑 Deteniendo servicios anteriores..."
pkill -f 'elegance-.*.jar' || true
sleep 5

# 3. Detectar binario de Java
JAVA_CMD=$(command -v java)
if [ -z "$JAVA_CMD" ]; then
    JAVA_CMD=$(find /usr/lib/jvm -name java -type f 2>/dev/null | head -n 1)
fi
echo "Usando Java: $JAVA_CMD"

cd /opt/elegance

# 4. Iniciar User Service (PUERTO 8082)
echo "🚀 Iniciando elegance-user-service..."
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8082 \
 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
 -Dspring.datasource.username=root \
 -Dspring.datasource.password="${DB_PASSWORD:-}" \
 -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
 -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
 -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
 -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST}" \
 -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
 -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
 -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
 elegance-user-service.jar </dev/null > user.log 2>&1 &

# 5. Iniciar Services Service (PUERTO 8084)
echo "🚀 Iniciando elegance-services-service..."
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8084 \
 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_services?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
 -Dspring.datasource.username=root \
 -Dspring.datasource.password="${DB_PASSWORD:-}" \
 -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
 -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
 -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
 -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST}" \
 -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
 -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
 -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
 elegance-services-service.jar </dev/null > services.log 2>&1 &

# 6. Iniciar Appointment Service (PUERTO 8081)
echo "🚀 Iniciando elegance-appointment-service..."
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8081 \
 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
 -Dspring.datasource.username=root \
 -Dspring.datasource.password="${DB_PASSWORD:-}" \
 -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
 -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
 -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
 -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST}" \
 -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
 -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
 -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
 elegance-appointment-service.jar </dev/null > appointment.log 2>&1 &

# 7. Iniciar Notification Service (PUERTO 8083)
echo "🚀 Iniciando elegance-notification-service..."
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8083 \
 -Dspring.mail.username="${MAIL_USERNAME:-}" \
 -Dspring.mail.password="${MAIL_PASSWORD:-}" \
 -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
 -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
 -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
 -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST}" \
 -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
 -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
 -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
 elegance-notification-service.jar </dev/null > notification.log 2>&1 &

echo "✅ Todos los servicios iniciados. Revisa los archivos .log en /opt/elegance/"