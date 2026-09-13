#!/bin/bash
set -e
source /opt/elegance/.env
pkill -f 'elegance-.*\.jar' || true
sleep 5
cd /opt/elegance

nohup java -Xmx256m -jar -Dserver.port=8082 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="$DB_PASSWORD" \
  elegance-user-service.jar > user.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8081 \
  -Dspring.datasource.url="jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dspring.datasource.username=root \
  -Dspring.datasource.password="$DB_PASSWORD" \
  elegance-appointment-service.jar > appointment.log 2>&1 &

nohup java -Xmx256m -jar -Dserver.port=8083 \
  -Dspring.mail.username="$MAIL_USERNAME" \
  -Dspring.mail.password="$MAIL_PASSWORD" \
  elegance-notification-service.jar > notification.log 2>&1 &

echo "✅ Servicios reiniciados"