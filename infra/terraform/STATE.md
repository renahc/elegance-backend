# Terraform state and safe deployment

The backend deployment workflow intentionally deploys JARs to the existing EC2
instance and does not run Terraform. The frontend workflow similarly deploys the
SPA without replacing its EC2 instance. This avoids creating duplicate resources
while the current AWS infrastructure is not yet represented in a persistent
Terraform state.

The Terraform configuration uses the S3 bucket `elegance-tf-state-2026-gabriel`
and DynamoDB lock table `elegance-tf-lock`. Both must exist in `us-east-1` and
the AWS identity must be allowed to access them before running `terraform init`.
Do not run `terraform apply` from CI or locally until the state has been
reconciled with the resources that are already running.

## Reconciliation checklist

1. Confirm the state bucket and lock table exist and are accessible in the
   currently active AWS Academy account.
2. Back up any existing local state file before initializing the S3 backend.
   If a valid local state exists, use `terraform init -migrate-state` from the
   machine that owns it. Do not initialize with migration from an ephemeral
   GitHub Actions runner.
3. If there is no trustworthy state, inventory the running EC2 instances,
   security groups, key pairs, API Gateway, routes, integrations, authorizer,
   and stage. Import matching resources into Terraform state before planning.
4. Run `terraform plan` and inspect every create, replace, and destroy action.
   Stop if Terraform proposes replacing or destroying any active service,
   database, broker, or API Gateway resource.
5. Only after the plan matches the intended infrastructure should an apply
   workflow be added or enabled.

The RabbitMQ EC2 user data still configures credentials during instance
provisioning. Changing those Terraform inputs changes user data and can replace
the broker instance, so rotate broker credentials separately and do not apply
that change without reviewing the Terraform plan and planning for data recovery.
Set `TF_VAR_rabbitmq_password` and `TF_VAR_rabbitmq_erlang_cookie` from protected
secrets when planning infrastructure changes; do not put their values in a
checked-in `.tfvars` file.
