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
    bucket         = "elegance-tf-state-2026-gabriel"
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
# LLAVE SSH
# ==========================================
resource "tls_private_key" "ec2_key" {
  algorithm = "RSA"
}

resource "aws_key_pair" "elegance_key" {
  key_name   = "${var.app_name}-key"
  public_key = tls_private_key.ec2_key.public_key_openssh
}

# ==========================================
# SECURITY GROUP
# ==========================================
resource "aws_security_group" "elegance_sg" {
  name        = "${var.app_name}-ec2-sg"
  description = "Permite SSH y puertos 8081-8083"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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
# AMI Amazon Linux 2 (OPTIMIZADO)
# ==========================================
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ==========================================
# INSTANCIA EC2 CON DOCKER (5x MÁS RÁPIDO)
# ==========================================
resource "aws_instance" "elegance_ec2" {
  ami             = data.aws_ami.amazon_linux.id
  instance_type   = var.instance_type
  key_name        = aws_key_pair.elegance_key.key_name
  security_groups = [aws_security_group.elegance_sg.name]

  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -e
    
    STATUS_FILE="/opt/elegance/status.txt"
    mkdir -p /opt/elegance
    chown -R ec2-user:ec2-user /opt/elegance
    echo "BOOTING" > $STATUS_FILE
    
    exec > >(tee -a /var/log/user-data.log) 2>&1
    echo "[$(date)] === Iniciando aprovisionamiento con Docker ==="
    
    # Actualizar repos (solo security updates - rápido)
    echo "[$(date)] Actualizando repos..."
    yum update -y --security-only 2>&1 | tail -3
    
    # Instalar Docker (mucho más rápido que Java nativo)
    echo "[$(date)] Instalando Docker..."
    amazon-linux-extras install docker -y
    systemctl enable docker
    systemctl start docker
    
    # Instalar Docker Compose
    echo "[$(date)] Instalando Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    
    # Crear directorio de aplicación
    mkdir -p /opt/elegance/app
    chown -R ec2-user:ec2-user /opt/elegance
    
    # Agregar ec2-user al grupo docker
    usermod -aG docker ec2-user
    
    echo "READY" > $STATUS_FILE
    echo "[$(date)] === Aprovisionamiento completado (Docker listo) ==="
  EOF
  )

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
# API GATEWAY
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
# INTEGRACIONES HTTP PROXY
# ==========================================
resource "aws_apigatewayv2_integration" "clients_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/clients"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "clients_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/clients/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "stylists_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/stylists"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "stylists_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/stylists/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "appointments_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8081/api/v1/appointments"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "appointments_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8081/api/v1/appointments/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "notifications_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8083/api/v1/notifications"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "notifications_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8083/api/v1/notifications/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

# ==========================================
# RUTAS DE API GATEWAY
# ==========================================
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


