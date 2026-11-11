#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/opt/elegance"
cd "$APP_DIR"

if [[ ! -f .env ]]; then
  echo "Missing $APP_DIR/.env" >&2
  exit 1
fi

set -a
source ./.env
set +a

: "${DB_PASSWORD:?DB_PASSWORD must be set in .env}"
: "${AZURE_AD_ISSUER_URI:?AZURE_AD_ISSUER_URI must be set in .env}"
: "${AZURE_AD_CLIENT_ID:?AZURE_AD_CLIENT_ID must be set in .env}"
: "${SPRING_RABBITMQ_HOST:?SPRING_RABBITMQ_HOST must be set in .env}"
: "${SPRING_RABBITMQ_PASSWORD:?SPRING_RABBITMQ_PASSWORD must be set in .env}"

JAVA_CMD="$(command -v java || true)"
if [[ -z "$JAVA_CMD" ]]; then
  echo "Java is not installed; refusing to restart services." >&2
  exit 1
fi

export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
export SPRING_RABBITMQ_PORT="${SPRING_RABBITMQ_PORT:-5672}"
export SPRING_RABBITMQ_USERNAME="${SPRING_RABBITMQ_USERNAME:-admin}"
export SPRING_MAIL_USERNAME="${MAIL_USERNAME:-}"
export SPRING_MAIL_PASSWORD="${MAIL_PASSWORD:-}"

stop_services() {
  pkill -f '[e]legance-(user|appointment|notification|services)-service\.jar' || true
}

cleanup_on_failure() {
  local status=$?
  if (( status != 0 )); then
    stop_services
  fi
}
trap cleanup_on_failure EXIT

start_service() {
  local name="$1"
  local port="$2"
  local jar="$3"
  local log="$4"
  local datasource_url="${5:-}"
  shift 5
  local -a jvm_args=("$@")
  local pid

  if [[ ! -f "$jar" ]]; then
    echo "Missing application artifact: $jar" >&2
    return 1
  fi

  if [[ -n "$datasource_url" ]]; then
    SPRING_DATASOURCE_URL="$datasource_url" \
      nohup "$JAVA_CMD" -Xmx256m "${jvm_args[@]}" -jar "$jar" \
      </dev/null >"$log" 2>&1 &
  else
    nohup "$JAVA_CMD" -Xmx256m "${jvm_args[@]}" -jar "$jar" \
      </dev/null >"$log" 2>&1 &
  fi
  pid=$!
  printf '%s\n' "$pid" >"${name}.pid"

  for _ in {1..60}; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$name exited during startup; see $APP_DIR/$log" >&2
      return 1
    fi
    if bash -c ":</dev/tcp/127.0.0.1/$port" 2>/dev/null; then
      echo "$name is listening on port $port"
      return 0
    fi
    sleep 2
  done

  echo "$name did not listen on port $port before timeout; see $APP_DIR/$log" >&2
  return 1
}

stop_services
sleep 2

start_service user 8082 elegance-user-service.jar user.log \
  "jdbc:mysql://localhost:3306/elegance_users?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dserver.port=8082

start_service appointment 8081 elegance-appointment-service.jar appointment.log \
  "jdbc:mysql://localhost:3306/elegance_appointments?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dserver.port=8081

start_service notification 8083 elegance-notification-service.jar notification.log "" \
  -Dserver.port=8083

start_service services 8084 elegance-services-service.jar services.log \
  "jdbc:mysql://localhost:3306/elegance_services?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
  -Dserver.port=8084

trap - EXIT
echo "All Spring services are listening."
