# Élégance backend

Java 17 / Spring Boot 3.2.3. Seven HTTP services share versioned event contracts in `elegance-common`.

| Service | Port | Responsibility |
| --- | --- | --- |
| appointment | 8081 | Reservations, ownership, time conflicts, transactional outbox |
| user | 8082 | Clients, current identity profile, stylists |
| notification | 8083 | RabbitMQ consumers and durable notifications |
| services | 8084 | Catalog and rates |
| analytics | 8085 | Kafka Streams pipeline and statistics |
| audit | 8086 | Independent Kafka consumer and durable event history |
| kafka-admin | 8087 | Topics, partitions, configuration, groups, lag, offsets, DLT replay |

## Run locally

```sh
cp .env.example .env
# Replace password placeholders and configure Entra issuer, audience and JWKS URI.
docker compose up -d --build
```

The default environment has one Kafka broker. For a fresh evaluation environment with three brokers:

```sh
docker compose -f docker-compose.yml -f docker-compose.cluster.yml up -d --build
```

Use replication factor 3 in `.env` for that environment. Existing topics do not acquire additional replicas simply by adding brokers; use a fresh environment or explicit replica reassignment. Each business topic has three partitions. The three brokers on one machine demonstrate broker failure, not EC2 host failure.

API ports bind only to localhost. Kafka UI: `http://localhost:8090`; RabbitMQ Management: `http://localhost:15672`. Credentials come from `.env`. The `demo` Spring profile explicitly inserts fictitious catalog and CRM data. Default execution does not seed data.

Run the React frontend from its separate repository. Vite proxies `/api/v1` to these local ports. Use HTTPS and register its origin as a SPA redirect URI in Entra.

## Event contracts

`contracts/salon-event.schema.json` and its examples define JSON v1. Message keys are appointment IDs. Events contain no client names or email addresses.

Business topics:

- `elegance.appointments.events.v1`: CREATED and STATUS_UPDATED.
- `elegance.services.completed.v1`: COMPLETED, once per transition to completed.

Auxiliary topics:

- `elegance.analytics.facts.v1`: validated records for the idempotent database projection.
- `elegance.analytics.windows.v1`: processing-record throughput per category in five-minute windows. This is an operational metric, not revenue.
- `elegance.streaming.invalid.v1`: invalid raw input rejected by the Streams pipeline.

Four `.DLT` topics are provisioned for the two business topics, facts and invalid input. Kafka also creates internal offset, transaction, repartition and changelog topics. Business events and facts have seven-day retention; DLTs have fourteen days. `docker/topics.sh` configures the topology explicitly.

The outbox stores business changes and pending messages in the same database transaction. Publishers confirm delivery independently for Kafka and RabbitMQ. A crash after send can redeliver an event; consumers persist unique event IDs and financial statistics deduplicate completed appointment IDs. Database side effects use at-least-once delivery with idempotence. Streams' exactly-once processing applies within Kafka, not to arbitrary external effects.

Statistics represent events received by this pipeline. They do not automatically include historical seeded appointments. Service value is not collected payment revenue.

## Security

Only catalog/stylist reads and health checks are public. Other business operations require a signed, unexpired JWT with matching issuer and audience. Administration requires an Entra app role whose value is exactly `Admin`. Clients can access only their own reservations and `/api/v1/clients/me`. Do not use Microsoft Graph tokens for this API; request its `access_as_user` scope.

`AZURE_AD_ISSUER_URI` must match the actual API token issuer (v1 or v2), and `AZURE_AD_JWK_SET_URI` must match the tenant. No authentication bypass is enabled in the normal compose file.

## Tests

```sh
mvn -f elegance-backend-parent/pom.xml verify
# Compiles JARs used by the prebuilt test Dockerfile.
cp .env.example .env.integration
docker compose --env-file .env.integration -f docker-compose.yml -f docker-compose.test.yml -p elegance-integration up -d --build
python3 scripts/test/integration.py
ELEGANCE_TEST_ENV=.env.integration python3 scripts/test/resilience.py
```

The test overlay runs a disposable RSA JWT issuer on localhost:19000. Never apply that overlay to a deployed environment. It tests actual JWT signature/issuer/audience/expiration validation independently of interactive Microsoft login.

`integration.py` checks roles, ownership, canonical pricing, conflicts, both Kafka topics, statistics, audit, Rabbit notifications, topic administration and lag. `resilience.py` requires a successful integration run first and checks duplicate delivery, DLQ, DLT and persistence across restarts.

`scripts/test/load.sh [count]` uses Kafka's producer performance tool against an isolated test topic, up to 100000 records. It reports confirmed sends and latency. The topic has one-hour retention. Set `KAFKA_CONCURRENCY` to increase listener threads; `docker-compose.scale.yml` removes the audit host-port binding so multiple instances can share its group.

## AWS

`infra/streaming` provisions a separate Ubuntu EC2 and restricted security group without terminating existing instances. Terraform requires an existing SSH key name and administrator CIDR. Choose instance capacity after measuring the selected broker profile.

Upload sources and `.env` separately to `/opt/elegance`; upload the frontend build to `/opt/elegance/public`. Set `APP_DOMAIN` to a DNS name pointing at EC2. Run `ELEGANCE_CLUSTER=true bash scripts/deploy-streaming.sh` for the three-broker profile. Caddy serves the frontend and API over HTTPS. Broker and database ports stay internal; dashboard ports bind to loopback and can be reached through SSH forwarding.

GitHub workflows validate PRs. Production deployment is an explicit workflow dispatch and uses `EC2_HOST`, `EC2_SSH_KEY`, `EC2_KNOWN_HOSTS` and optional `EC2_USER`. They do not recreate instances or run Terraform automatically. The environment file stays on the server. Public GitHub visibility, Microsoft app configuration and real-user login must be verified separately before handoff.
