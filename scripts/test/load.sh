#!/usr/bin/env bash
# Isolated throughput experiment; never writes business events or invokes public APIs.
set -euo pipefail
cd "$(dirname "$0")/../.."
records="${1:-10000}"
if ! [[ "$records" =~ ^[0-9]+$ ]] || [ "$records" -lt 1 ] || [ "$records" -gt 100000 ]; then
 echo 'Use a count between 1 and 100000' >&2; exit 1
fi
compose=(docker compose --env-file "${ELEGANCE_TEST_ENV:-.env}" -f docker-compose.yml -p "${ELEGANCE_PROJECT:-elegance}")
"${compose[@]}" exec -T kafka1 kafka-topics --bootstrap-server kafka1:29092 --create --if-not-exists --topic elegance.load.events.v1 --partitions 3 --replication-factor "${KAFKA_REPLICATION_FACTOR:-1}" --config retention.ms=3600000 --config retention.bytes=67108864 --config segment.ms=60000
"${compose[@]}" exec -T kafka1 kafka-producer-perf-test --topic elegance.load.events.v1 --num-records "$records" --record-size 1024 --throughput 2000 --producer-props bootstrap.servers=kafka1:29092 acks=all enable.idempotence=true batch.size=32768 linger.ms=20 compression.type=snappy
