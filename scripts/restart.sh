#!/bin/bash
set -e

if [ -f /opt/elegance/.env ]; then
  set -a
  source /opt/elegance/.env
  set +a
fi

pkill -f 'elegance-.*\.jar' || true
sleep 5
if [ -z "$JAVA_HOME" ]; then
  if [ -d "/usr/lib/jvm/java-17-amazon-corretto" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto"
  elif [ -d "/usr/lib/jvm/java-17-amazon-corretto.x86_64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-17-amazon-corretto.x86_64"
  fi
fi
if [ -n "$JAVA_HOME" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

cd /opt/elegance

nohup java -Xmx256m -jar -Dserver.port=8082 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-user-service.jar </dev/null > user.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8081 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="${DB_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-appointment-service.jar </dev/null > appointment.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8083 \
  -Dspring.mail.username="${MAIL_USERNAME:-}" \
  -Dspring.mail.password="${MAIL_PASSWORD:-}" \
  -Dazure.activedirectory.issuer-uri="${AZURE_AD_ISSUER_URI:-https://sts.windows.net/7607c5a6-994c-4951-92bc-3af0cf3eb713/}" \
  -Dazure.activedirectory.client-id="${AZURE_AD_CLIENT_ID:-api://304d54f7-d485-478a-a1ea-0c2f874b0c1f}" \
  elegance-notification-service.jar </dev/null > notification.log 2>&1 &

sleep 5
echo "=== Procesos Java en ejecución ==="
ps aux | grep java || true
echo "✅ Servicios reiniciados"