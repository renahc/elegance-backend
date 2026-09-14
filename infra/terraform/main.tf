terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    bucket         = "elegance-tf-state-2026-gabriel" # Tu bucket real
    key            = "elegance/ec2/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "elegance-tf-lock"
  }
}

provider "aws" {
  region = var.aws_region
}

# ==========================================
# LLAVE SSH (generada por Terraform)
# ==========================================
resource "tls_private_key" "ec2_key" {
  algorithm = "RSA"
}

resource "aws_key_pair" "elegance_key" {
  key_name   = "${var.app_name}-key"
  public_key = tls_private_key.ec2_key.public_key_openssh
}

# ==========================================
# SECURITY GROUP (SSH + puertos de los microservicios)
# ==========================================
resource "aws_security_group" "elegance_sg" {
  name        = "${var.app_name}-ec2-sg"
  description = "Permite SSH y puertos 8081-8083"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # En prod, restringe a tu IP
  }

  ingress {
    from_port   = 8081
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ==========================================
# AMI Amazon Linux 2023
# ==========================================
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ==========================================
# INSTANCIA EC2 (Java 17 + MySQL instalados al arrancar)
# ==========================================
resource "aws_instance" "elegance_ec2" {
  ami             = data.aws_ami.amazon_linux.id
  instance_type   = var.instance_type
  key_name        = aws_key_pair.elegance_key.key_name
  security_groups = [aws_security_group.elegance_sg.name]

  user_data = <<-EOF
    #!/bin/bash
    set -ex
    mkdir -p /opt/elegance
    chown -R ec2-user:ec2-user /opt/elegance
    echo "BOOTING" > /opt/elegance/status.txt

    exec > >(tee /var/log/user-data.log | logger -t user-data -s 2>/dev/console) 2>&1

    echo "=== Instalando Java 17 y MariaDB en AL2023 ==="
    dnf install -y java-17-amazon-corretto mariadb105-server
    
    echo "=== Iniciando y habilitando servicio MariaDB ==="
    systemctl enable --now mariadb
    sleep 5
    
    echo "=== Configurando usuario root y bases de datos ==="
    mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${var.db_password}'; FLUSH PRIVILEGES;"
    mysql -u root -p'${var.db_password}' -e "CREATE DATABASE IF NOT EXISTS elegance_users; CREATE DATABASE IF NOT EXISTS elegance_appointments;"
    
    echo "READY" > /opt/elegance/status.txt
    echo "=== user_data completado exitosamente ==="
  EOF

  tags = {
    Name = "${var.app_name}-ec2"
  }
}

# ==========================================
# COGNITO USER POOL & CLIENT
# ==========================================
resource "aws_cognito_user_pool" "elegance_pool" {
  name                     = "${var.app_name}-user-pool"
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
    require_uppercase = true
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  tags = {
    Name = "${var.app_name}-cognito-pool"
  }
}

resource "aws_cognito_user_pool_client" "elegance_client" {
  name         = "${var.app_name}-app-client"
  user_pool_id = aws_cognito_user_pool.elegance_pool.id

  generate_secret = false

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH"
  ]

  prevent_user_existence_errors = "ENABLED"
}

# ==========================================
# API GATEWAY (HTTP API v2)
# ==========================================
resource "aws_apigatewayv2_api" "elegance_api" {
  name          = "${var.app_name}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["*"]
    allow_headers = ["*"]
    max_age       = 300
  }

  tags = {
    Name = "${var.app_name}-api-gateway"
  }
}

# ==========================================
# COGNITO JWT AUTHORIZER
# ==========================================
resource "aws_apigatewayv2_authorizer" "cognito_auth" {
  api_id           = aws_apigatewayv2_api.elegance_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "${var.app_name}-cognito-authorizer"

  jwt_configuration {
    audience = [aws_cognito_user_pool_client.elegance_client.id]
    issuer   = "https://${aws_cognito_user_pool.elegance_pool.endpoint}"
  }
}


resource "null_resource" "wait_for_ec2" {
  provisioner "remote-exec" {
    inline = ["echo 'EC2 is ready'"]

    connection {
      type        = "ssh"
      user        = "ec2-user"
      private_key = tls_private_key.ec2_key.private_key_pem
      host        = aws_instance.elegance_ec2.public_ip
      timeout     = "5m"
    }
  }

  depends_on = [aws_instance.elegance_ec2]
}


# ==========================================
# INTEGRACIONES HTTP PROXY HACIA EC2
# ==========================================
# User Service (Puerto 8082): Clientes
resource "aws_apigatewayv2_integration" "clients_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/clients"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "clients_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/clients/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

# User Service (Puerto 8082): Estilistas
resource "aws_apigatewayv2_integration" "stylists_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/stylists"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "stylists_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/stylists/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

# Appointment Service (Puerto 8081): Citas
resource "aws_apigatewayv2_integration" "appointments_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8081/api/v1/appointments"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "appointments_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8081/api/v1/appointments/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

# Notification Service (Puerto 8083): Notificaciones
resource "aws_apigatewayv2_integration" "notifications_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8083/api/v1/notifications"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "notifications_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8083/api/v1/notifications/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
}
resource "aws_apigatewayv2_integration" "user_service_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/%7Bproxy%7D"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"

  depends_on = [null_resource.wait_for_ec2]
}
resource "aws_apigatewayv2_integration" "appointment_service_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8081/%7Bproxy%7D"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"

  depends_on = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "notification_service_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8083/%7Bproxy%7D"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"

  depends_on = [null_resource.wait_for_ec2]
}
# ==========================================
# RUTAS DE API GATEWAY (PROTEGIDAS CON COGNITO)
# ==========================================
# Clientes (User Service: 8082)
resource "aws_apigatewayv2_route" "clients_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/clients"
  target             = "integrations/${aws_apigatewayv2_integration.clients_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

resource "aws_apigatewayv2_route" "clients_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/clients/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.clients_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

# Estilistas (User Service: 8082)
resource "aws_apigatewayv2_route" "stylists_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/stylists"
  target             = "integrations/${aws_apigatewayv2_integration.stylists_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

resource "aws_apigatewayv2_route" "stylists_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/stylists/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.stylists_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

# Citas y Agenda (Appointment Service: 8081)
resource "aws_apigatewayv2_route" "appointments_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/appointments"
  target             = "integrations/${aws_apigatewayv2_integration.appointments_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

resource "aws_apigatewayv2_route" "appointments_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/appointments/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.appointments_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

# Notificaciones (Notification Service: 8083)
resource "aws_apigatewayv2_route" "notifications_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/notifications"
  target             = "integrations/${aws_apigatewayv2_integration.notifications_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

resource "aws_apigatewayv2_route" "notifications_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/notifications/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.notifications_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.cognito_auth.id
}

# ==========================================
# STAGE DEPLOYMENT
# ==========================================
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.elegance_api.id
  name        = "$default"
  auto_deploy = true

  tags = {
    Name = "${var.app_name}-default-stage"
  }
}