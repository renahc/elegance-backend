variable "aws_region" {
  description = "Región de AWS"
  type        = string
  default     = "us-east-1"
}

variable "app_name" {
  description = "Nombre de la aplicación"
  type        = string
  default     = "elegance"
}

variable "ecr_user_service_uri" {
  description = "URI de la imagen de User Service en ECR"
  type        = string
}

variable "db_url" {
  description = "URL de la base de datos"
  type        = string
}

variable "db_username" {
  description = "Usuario de la base de datos"
  type        = string
}

variable "db_password" {
  description = "Contraseña de la base de datos"
  type        = string
  sensitive   = true
}

variable "azure_tenant_id" {
  type      = string
  sensitive = true
}

variable "azure_client_id" {
  type      = string
  sensitive = true
}

variable "azure_client_secret" {
  type      = string
  sensitive = true
}