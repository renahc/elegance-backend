#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test -f .env || { echo "Falta .env en el servidor" >&2; exit 1; }
chmod 600 .env
files=(-f docker-compose.yml)
if [ "${ELEGANCE_CLUSTER:-false}" = true ]; then files+=(-f docker-compose.cluster.yml); fi
docker compose "${files[@]}" --profile web config --quiet
docker compose "${files[@]}" --profile web up -d --build --remove-orphans
deadline=$((SECONDS + 600))
for port in 8081 8082 8083 8084 8085 8086 8087; do
  until curl --fail --silent --max-time 5 "http://127.0.0.1:$port/actuator/health" >/dev/null; do
    if [ "$SECONDS" -ge "$deadline" ]; then
      echo "Service on port $port did not become healthy" >&2
      docker compose "${files[@]}" ps
      exit 1
    fi
    sleep 3
  done
done
docker compose "${files[@]}" ps
