#!/bin/bash
set -e

if [ -f /opt/elegance/.env ]; then
  set -a
  source /opt/elegance/.env
  set +a
fi

pkill -f 'elegance-.*\.jar' || true
sleep 5
JAVA_CMD=""
if command -v java >/dev/null 2>&1; then
  JAVA_CMD="java"
elif [ -f /usr/bin/java ]; then
  JAVA_CMD="/usr/bin/java"
elif [ -f /usr/lib/jvm/java-17-amazon-corretto/bin/java ]; then
  JAVA_CMD="/usr/lib/jvm/java-17-amazon-corretto/bin/java"
elif [ -f /usr/lib/jvm/java-17-amazon-corretto.x86_64/bin/java ]; then
  JAVA_CMD="/usr/lib/jvm/java-17-amazon-corretto.x86_64/bin/java"
else
  JAVA_CMD=$(find /usr/lib/jvm -name java -type f 2>/dev/null | head -n 1)
fi

if [ -z "$JAVA_CMD" ]; then
  echo "⚠️ Java binary not found on the instance! Attempting emergency installation of Java 17..."
  sudo amazon-linux-extras install -y corretto17 || (
    sudo rpm --import https://yum.corretto.aws/corretto.key && \
    sudo curl -L -s -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo && \
    sudo yum install -y java-17-amazon-corretto-devel || sudo yum install -y java-17-amazon-corretto
  ) || true

  if command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
  elif [ -f /usr/bin/java ]; then
    JAVA_CMD="/usr/bin/java"
  else
    JAVA_CMD=$(find /usr/lib/jvm -name java -type f 2>/dev/null | head -n 1)
  fi
fi

if [ -z "$JAVA_CMD" ]; then
  echo "❌ Error: Java binary not found on the instance even after emergency install!"
  exit 1
fi

echo "Using Java binary: $JAVA_CMD"

cd /opt/elegance

# 1. User Service (Puerto 8082)
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8082 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-user-service.jar </dev/null > user.log 2>&1 &

# 2. Appointment Service (Puerto 8081 - Publicador RabbitMQ)
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8081 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST:-localhost}" \
  -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
  -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
  -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
  elegance-appointment-service.jar </dev/null > appointment.log 2>&1 &

# 3. Notification Service (Puerto 8083 - Consumidor RabbitMQ)
nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8083 \
  -Dspring.mail.username="${MAIL_USERNAME:-}" \
  -Dspring.mail.password="${MAIL_PASSWORD:-}" \
  -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  -Dspring.rabbitmq.host="${SPRING_RABBITMQ_HOST:-localhost}" \
  -Dspring.rabbitmq.port="${SPRING_RABBITMQ_PORT:-5672}" \
  -Dspring.rabbitmq.username="${SPRING_RABBITMQ_USERNAME:-admin}" \
  -Dspring.rabbitmq.password="${SPRING_RABBITMQ_PASSWORD:-admin123}" \
  elegance-notification-service.jar </dev/null > notification.log 2>&1 &

# 4. Services Service (Puerto 8084)
if [ -f elegance-services-service.jar ]; then
  nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8084 \
    -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_services?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
    -Dspring.datasource.username=root \
    -Dspring.datasource.password="${DB_PASSWORD:-}" \
    -Dazure.activedirectory.tenant-id="${AZURE_AD_TENANT_ID:-7607c5a6-994c-4951-92bc-3af0cf3eb713}" \
    -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0}" \
    -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
    elegance-services-service.jar </dev/null > services.log 2>&1 &
fi

sleep 5
echo "=== Procesos Java en ejecución ==="
ps aux | grep java || true
echo "✅ Servicios reiniciados"