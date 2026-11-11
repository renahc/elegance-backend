variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "app_name" {
  type    = string
  default = "elegance"
}

variable "instance_type" {
  description = "t3.small = 2GB RAM (necesario para 3 apps Java + MySQL)"
  type        = string
  default     = "t3.small"
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "mail_username" {
  type      = string
  sensitive = true
}

variable "mail_password" {
  type      = string
  sensitive = true
}

variable "rabbitmq_password" {
  type      = string
  sensitive = true
}

variable "rabbitmq_erlang_cookie" {
  type      = string
  sensitive = true
}

variable "azure_ad_issuer_uri" {
  description = "Issuer exacto de los access tokens de la API de Entra ID"
  type        = string
  default     = "https://login.microsoftonline.com/7607c5a6-994c-4951-92bc-3af0cf3eb713/v2.0"
}