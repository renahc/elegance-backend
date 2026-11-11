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