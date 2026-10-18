#!/bin/bash
# ==========================================
# RESTART SCRIPT - Élégance Backend
# ==========================================

set -e

APP_DIR="/opt/elegance"
LOG_DIR="/opt/elegance"

echo "🔄 Deteniendo servicios existentes..."
pkill -f elegance-user-service.jar || true
pkill -f elegance-appointment-service.jar || true
pkill -f elegance-notification-service.jar || true
pkill -f elegance-services-service.jar || true
sleep 3

echo "📦 Cargando variables de entorno desde .env..."
# IMPORTANTE: Exportar todas las variables del .env
set -a
source /opt/elegance/.env
set +a

echo "🚀 Iniciando User Service (puerto 8082)..."
nohup java -jar $APP_DIR/elegance-user-service.jar \
  > $LOG_DIR/user.log 2>&1 &

echo "🚀 Iniciando Appointment Service (puerto 8081)..."
nohup java -jar $APP_DIR/elegance-appointment-service.jar \
  > $LOG_DIR/appointment.log 2>&1 &

echo "🚀 Iniciando Notification Service (puerto 8083)..."
nohup java -jar $APP_DIR/elegance-notification-service.jar \
  > $LOG_DIR/notification.log 2>&1 &

if [ -f $APP_DIR/elegance-services-service.jar ]; then
  echo "🚀 Iniciando Services Service (puerto 8084)..."
  nohup java -jar $APP_DIR/elegance-services-service.jar \
    > $LOG_DIR/services.log 2>&1 &
fi

echo "✅ Servicios iniciados. Esperando 10 segundos..."
sleep 10

echo "📋 Logs de Appointment Service (últimas 30 líneas):"
tail -n 30 $LOG_DIR/appointment.log || true

echo "📋 Variables de entorno RabbitMQ cargadas:"
env | grep RABBIT || echo "⚠️ No se encontraron variables RABBIT"