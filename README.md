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

For local development, API and dashboard ports bind only to localhost by default (`PUBLIC_BIND_ADDRESS=127.0.0.1`). Kafka UI: `http://localhost:8090`; RabbitMQ Management: `http://localhost:15672`. Credentials come from `.env`. The `demo` Spring profile explicitly inserts fictitious catalog and CRM data. Default execution does not seed data.

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

`infra/streaming` provisions one Ubuntu EC2 for the current Docker Compose stack, including its three-broker Kafka profile. It is a single-host deployment, not EC2-level high availability. The deploy workflow creates a temporary SSH key and restricts SSH ingress to the GitHub runner's `/32` address.

On each deploy to `kafka-ev3`, GitHub Actions terminates only the existing `elegance-streaming-ec2` instance and its `elegance-streaming-*` security groups in the default VPC, removes the deployment key pair, and provisions a fresh EC2. It then uploads the source and a generated `.env` and runs `scripts/deploy-streaming.sh`. Database and Kafka volumes are on that EC2, so replacing it loses their data. Other AWS Academy resources are not removed.

For evaluation, the deployment sets `PUBLIC_BIND_ADDRESS=0.0.0.0` and exposes Swagger on ports 8081-8084, RabbitMQ Management on 15672, and Kafka UI on 8090 to the public internet. RabbitMQ and Kafka UI require the `RABBITMQ_PASSWORD` and `DASHBOARD_PASSWORD` GitHub secrets (username `elegance`). AMQP and Kafka broker ports remain private. These HTTP endpoints are intended only for the short evaluation window; stop or terminate the EC2 afterwards.

GitHub Actions runs Maven, the Docker integration suite, and Terraform configuration validation on pushes and pull requests. The deploy job requires the `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN` repository secrets. Optional configuration uses the `AWS_REGION`, `APP_DOMAIN`, and `CORS_ALLOWED_ORIGINS` repository variables; `APP_DOMAIN` defaults to HTTP on port 80. Azure AD and mail settings can be supplied as repository secrets; Azure AD defaults come from `.env.example`.

The deploy job runs on pushes to `kafka-ev3` or on a manual dispatch with `deploy` enabled. The `production` environment can require a reviewer.
