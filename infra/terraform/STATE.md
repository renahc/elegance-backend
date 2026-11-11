# Terraform state and CI deployment

Both deployment workflows recreate only the resources declared by their
repository's `infra/terraform/main.tf`. Each run first removes matching
Élégance resources by their current AWS names/tags, then initializes Terraform
with `-backend=false` and creates a fresh stack using temporary local state.
The backend does not require the application EC2 instance to stay online, and
the workflows do not create or depend on an S3 state bucket.

The backend workflow also removes its API Gateway, backend and RabbitMQ EC2
instances, security groups, and generated key pair. The frontend workflow
removes its EC2 instance, security group, and generated key pair. Terminating
the instances permanently deletes their local MariaDB/RabbitMQ data. Every push
to the configured branch triggers this destructive recreation.

The cleanup scripts operate in the AWS account selected by the configured
credentials and the region selected by the `AWS_REGION` repository variable
(default `us-east-1`). They only target the resource names/prefixes corresponding
to the current and legacy Terraform configurations. If `app_name`, resource
names, or region change in Terraform, update the matching cleanup script in the
same change. Do not broaden the filters to all account resources.

Configure these GitHub repository secrets in **both** repositories:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN`

AWS Academy credentials are temporary and must be refreshed in GitHub before
they expire. The backend repository additionally requires these secrets:
`TEST_DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
`SPRING_RABBITMQ_PASSWORD`, `RABBITMQ_ERLANG_COOKIE`,
`AZURE_AD_ISSUER_URI`, and `AZURE_AD_CLIENT_ID`. The frontend repository
requires the `VITE_API_BASE_URL` secret and the `AZURE_CLIENT_ID`,
`AZURE_TENANT_ID`, and `AZURE_API_SCOPE` repository variables. Terraform state,
plan files, and generated SSH private keys remain only on the ephemeral
GitHub-hosted runner and must not be uploaded as artifacts.

The backend workflow prints the newly generated backend/RabbitMQ IPs and API
Gateway URL in the Actions run summary. Since each backend recreation changes
the API Gateway URL, update the frontend repository's `VITE_API_BASE_URL`
secret before its next deployment.
