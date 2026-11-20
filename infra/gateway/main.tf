terraform {
  required_version = ">= 1.9.0"
  required_providers { aws = { source = "hashicorp/aws", version = "~> 5.0" } }
}
variable "azure_tenant_id" {
  type    = string
  default = "7607c5a6-994c-4951-92bc-3af0cf3eb713"
}
variable "azure_client_id" {
  type    = string
  default = "304d54f7-d485-478a-a1ea-0c2f874b0c1f"
}
variable "frontend_origins" {
  type    = list(string)
  default = []
}
variable "backend_domain" {
  type = string
}
locals {
  jwt_issuer = "https://login.microsoftonline.com/${var.azure_tenant_id}/v2.0"
}
resource "aws_apigatewayv2_api" "streaming" {
  name          = "elegance-streaming-http-api"
  protocol_type = "HTTP"
  cors_configuration {
    allow_origins  = distinct(concat(["https://${var.backend_domain}", "https://localhost:5173"], var.frontend_origins))
    allow_methods  = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers  = ["Authorization", "Content-Type", "X-Correlation-ID"]
    expose_headers = ["X-Correlation-ID"]
    max_age        = 300
  }
}
resource "aws_apigatewayv2_authorizer" "entra" {
  api_id           = aws_apigatewayv2_api.streaming.id
  name             = "microsoft-entra-jwt"
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  jwt_configuration {
    issuer   = local.jwt_issuer
    audience = [var.azure_client_id]
  }
}
resource "aws_apigatewayv2_integration" "streaming_root" {
  api_id                 = aws_apigatewayv2_api.streaming.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "https://${var.backend_domain}"
  payload_format_version = "1.0"
  request_parameters = {
    "overwrite:path" = "$request.path"
  }
}
resource "aws_apigatewayv2_integration" "streaming_proxy" {
  api_id                 = aws_apigatewayv2_api.streaming.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "https://${var.backend_domain}"
  payload_format_version = "1.0"
  request_parameters = {
    "overwrite:path" = "$request.path"
  }
}
resource "aws_apigatewayv2_route" "streaming_root" {
  api_id    = aws_apigatewayv2_api.streaming.id
  route_key = "ANY /"
  target    = "integrations/${aws_apigatewayv2_integration.streaming_root.id}"
}
resource "aws_apigatewayv2_route" "streaming_proxy" {
  api_id    = aws_apigatewayv2_api.streaming.id
  route_key = "ANY /{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.streaming_proxy.id}"
}
resource "aws_apigatewayv2_route" "protected" {
  for_each = toset([
    "ANY /api/v1", "ANY /api/v1/{proxy+}",
    "ANY /api/v1/services", "ANY /api/v1/services/{proxy+}",
    "ANY /api/v1/stylists", "ANY /api/v1/stylists/{proxy+}",
  ])
  api_id               = aws_apigatewayv2_api.streaming.id
  route_key            = each.value
  target               = "integrations/${aws_apigatewayv2_integration.streaming_proxy.id}"
  authorization_type   = "JWT"
  authorizer_id        = aws_apigatewayv2_authorizer.entra.id
  authorization_scopes = ["access_as_user"]
}
resource "aws_apigatewayv2_route" "catalog" {
  for_each = toset([
    "GET /api/v1/services", "GET /api/v1/services/{proxy+}",
    "GET /api/v1/stylists", "GET /api/v1/stylists/{proxy+}",
  ])
  api_id             = aws_apigatewayv2_api.streaming.id
  route_key          = each.value
  target             = "integrations/${aws_apigatewayv2_integration.streaming_proxy.id}"
  authorization_type = "NONE"
}
moved {
  from = aws_apigatewayv2_route.preflight
  to   = aws_apigatewayv2_route.preflight["OPTIONS /{proxy+}"]
}
resource "aws_apigatewayv2_route" "preflight" {
  for_each = toset([
    "OPTIONS /{proxy+}", "OPTIONS /api/v1", "OPTIONS /api/v1/{proxy+}",
    "OPTIONS /api/v1/services", "OPTIONS /api/v1/services/{proxy+}",
    "OPTIONS /api/v1/stylists", "OPTIONS /api/v1/stylists/{proxy+}",
  ])
  api_id             = aws_apigatewayv2_api.streaming.id
  route_key          = each.value
  target             = "integrations/${aws_apigatewayv2_integration.streaming_proxy.id}"
  authorization_type = "NONE"
}
resource "aws_apigatewayv2_stage" "streaming" {
  api_id      = aws_apigatewayv2_api.streaming.id
  name        = "$default"
  auto_deploy = true
}

output "api_gateway_url" {
  value = trimsuffix(aws_apigatewayv2_stage.streaming.invoke_url, "/")
}
output "jwt_issuer" {
  value = local.jwt_issuer
}
