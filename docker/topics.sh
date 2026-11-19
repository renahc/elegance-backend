#!/usr/bin/env bash
set -euo pipefail
rf="${KAFKA_REPLICATION_FACTOR:-1}"
isr=1
if [ "$rf" -gt 1 ]; then isr=2; fi
for topic in elegance.appointments.events.v1 elegance.services.completed.v1 elegance.analytics.facts.v1 elegance.streaming.invalid.v1; do
 kafka-topics --bootstrap-server kafka1:29092 --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor "$rf" --config retention.ms=604800000 --config retention.bytes=268435456 --config cleanup.policy=delete --config min.insync.replicas="$isr"
 kafka-topics --bootstrap-server kafka1:29092 --create --if-not-exists --topic "$topic.DLT" --partitions 3 --replication-factor "$rf" --config retention.ms=1209600000 --config cleanup.policy=delete --config min.insync.replicas="$isr"
done
kafka-topics --bootstrap-server kafka1:29092 --create --if-not-exists --topic elegance.analytics.windows.v1 --partitions 3 --replication-factor "$rf" --config cleanup.policy=compact,delete --config retention.ms=604800000 --config min.insync.replicas="$isr"
