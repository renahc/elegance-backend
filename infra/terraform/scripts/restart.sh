#!/bin/bash
cd /opt/elegance

# 1. Cargar variables del .env si existe
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

# 2. Detener servicios anteriores
pkill -f "elegance-.*-service"
sleep 3

# 3. Variables por defecto si no están en el .env
RABBIT_HOST=${SPRING_RABBITMQ_HOST:-98.80.6.181} # <--- CAMBIA ESTA IP SI ES NECESARIO
RABBIT_PORT=${SPRING_RABBITMQ_PORT:-5672}
RABBIT_USER=${SPRING_RABBITMQ_USERNAME:-admin}
RABBIT_PASS=${SPRING_RABBITMQ_PASSWORD:-admin123}

DB_PASS=${DB_PASSWORD:-testpassword123}
TENANT_ID="7607c5a6-994c-4951-92bc-3af0cf3eb713"
CLIENT_ID="304d54f7-d485-478a-a1ea-0c2f874b0c1f"
ISSUER_URI="https://sts.windows.net/${TENANT_ID}/"

echo "🚀 Iniciando servicios con RabbitMQ en: $RABBIT_HOST"

# 4. Iniciar con logs
nohup java -Xmx256m -jar -Dserver.port=8082 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" -Dspring.datasource.username=root -Dspring.datasource.password=$DB_PASS -Dazure.activedirectory.tenant-id=$TENANT_ID -Dazure.activedirectory.issuer-uri=$ISSUER_URI -Dazure.activedirectory.client-id=$CLIENT_ID -Dspring.rabbitmq.host=$RABBIT_HOST -Dspring.rabbitmq.port=$RABBIT_PORT -Dspring.rabbitmq.username=$RABBIT_USER -Dspring.rabbitmq.password=$RABBIT_PASS elegance-user-service.jar > user.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8084 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_services?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" -Dspring.datasource.username=root -Dspring.datasource.password=$DB_PASS -Dazure.activedirectory.tenant-id=$TENANT_ID -Dazure.activedirectory.issuer-uri=$ISSUER_URI -Dazure.activedirectory.client-id=$CLIENT_ID -Dspring.rabbitmq.host=$RABBIT_HOST -Dspring.rabbitmq.port=$RABBIT_PORT -Dspring.rabbitmq.username=$RABBIT_USER -Dspring.rabbitmq.password=$RABBIT_PASS elegance-services-service.jar > services.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8081 -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" -Dspring.datasource.username=root -Dspring.datasource.password=$DB_PASS -Dazure.activedirectory.tenant-id=$TENANT_ID -Dazure.activedirectory.issuer-uri=$ISSUER_URI -Dazure.activedirectory.client-id=$CLIENT_ID -Dspring.rabbitmq.host=$RABBIT_HOST -Dspring.rabbitmq.port=$RABBIT_PORT -Dspring.rabbitmq.username=$RABBIT_USER -Dspring.rabbitmq.password=$RABBIT_PASS elegance-appointment-service.jar > appointment.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8083 -Dspring.mail.username=gabrielgamer498@gmail.com -Dspring.mail.password="jnkn ieyh mynf xsdz" -Dazure.activedirectory.tenant-id=$TENANT_ID -Dazure.activedirectory.issuer-uri=$ISSUER_URI -Dazure.activedirectory.client-id=$CLIENT_ID -Dspring.rabbitmq.host=$RABBIT_HOST -Dspring.rabbitmq.port=$RABBIT_PORT -Dspring.rabbitmq.username=$RABBIT_USER -Dspring.rabbitmq.password=$RABBIT_PASS elegance-notification-service.jar > notification.log 2>&1 &

echo "✅ Servicios iniciados. Revisa los archivos .log para ver el estado."