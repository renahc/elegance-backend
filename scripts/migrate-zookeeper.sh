#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
compose=(docker compose -f docker-compose.yml -f docker-compose.cluster.yml)
original=$("${compose[@]}" ps -a -q zookeeper)
if [ -z "$original" ]; then exit 0; fi
if docker inspect "$original" --format '{{range .Config.Env}}{{println .}}{{end}}' | grep -q '^ZOOKEEPER_SERVERS='; then
  exit 0
fi
backup="backups/zookeeper-$(date -u +%Y%m%dT%H%M%SZ)"
install -d -m 700 "$backup/data" "$backup/log"
"${compose[@]}" stop kafka1 kafka2 kafka3 zookeeper
docker cp "$original:/var/lib/zookeeper/data/." "$backup/data/"
docker cp "$original:/var/lib/zookeeper/log/." "$backup/log/"
# Seed both joining members from the stopped node to retain Kafka's metadata.
"${compose[@]}" create zookeeper2 zookeeper3
for node in zookeeper2 zookeeper3; do
  target=$("${compose[@]}" ps -a -q "$node")
  docker cp "$backup/data/." "$target:/var/lib/zookeeper/data/"
  docker cp "$backup/log/." "$target:/var/lib/zookeeper/log/"
  number="${node#zookeeper}"
  docker run --rm --user 0 --volumes-from "$target" --entrypoint sh \
    confluentinc/cp-zookeeper:7.9.0 \
    -c 'printf "%s\n" "$1" > /var/lib/zookeeper/data/myid
        chown -R 1000:1000 /var/lib/zookeeper/data /var/lib/zookeeper/log' sh "$number"
done
printf 'ZooKeeper metadata backup saved in %s\n' "$backup"
