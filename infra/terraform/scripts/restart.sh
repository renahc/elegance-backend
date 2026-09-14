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
  echo "❌ Error: Java binary not found on the instance!"
  exit 1
fi

echo "Using Java binary: $JAVA_CMD"

cd /opt/elegance

nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8082 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-user-service.jar </dev/null > user.log 2>&1 &

nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8081 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-appointment-service.jar </dev/null > appointment.log 2>&1 &

nohup $JAVA_CMD -Xmx256m -jar -Dserver.port=8083 \
  -Dspring.mail.username="${MAIL_USERNAME:-}" \
  -Dspring.mail.password="${MAIL_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-notification-service.jar </dev/null > notification.log 2>&1 &

sleep 5
echo "=== Procesos Java en ejecución ==="
ps aux | grep java || true
echo "✅ Servicios reiniciados"