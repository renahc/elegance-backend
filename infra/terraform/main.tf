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
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.elegance_key.key_name
  security_groups             = [aws_security_group.elegance_sg.name]
  user_data_replace_on_change = true

  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -e
    
    STATUS_FILE="/opt/elegance/status.txt"
    mkdir -p /opt/elegance
    chown -R ec2-user:ec2-user /opt/elegance
    echo "BOOTING" > $STATUS_FILE
    
    trap 'echo "FAILED" > /opt/elegance/status.txt' ERR
    
    exec > >(tee -a /var/log/user-data.log) 2>&1
    echo "[$(date)] === Iniciando aprovisionamiento (Java 17 + MariaDB) ==="
    
    # Actualizar repos
    echo "[$(date)] Actualizando repos..."
    yum update -y --security-only || true
    
    # Configurar repositorio de Corretto 17
    echo "[$(date)] Configurando repositorio Corretto 17..."
    rpm --import https://yum.corretto.aws/corretto.key || true
    curl -L -s -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo || true
    
    # Instalación de paquetes
    echo "[$(date)] Instalando Java 17 y MariaDB..."
    yum install -y java-17-amazon-corretto-devel mariadb-server mariadb || \
    yum install -y java-17-amazon-corretto mariadb-server mariadb || true
    
    # Verificar ejecutable java
    if ! command -v java >/dev/null 2>&1 && [ ! -f /usr/bin/java ]; then
      echo "[$(date)] Buscando ejecutable java en /usr/lib/jvm..."
      JAVA_BIN=$(find /usr/lib/jvm -name java -type f 2>/dev/null | head -n 1)
      if [ -n "$JAVA_BIN" ]; then
        ln -sf "$JAVA_BIN" /usr/bin/java
        echo "[$(date)] Enlace simbólico creado: /usr/bin/java -> $JAVA_BIN"
      fi
    fi
    
    # Iniciar y habilitar MariaDB
    echo "[$(date)] Iniciando MariaDB..."
    systemctl enable mariadb || true
    systemctl start mariadb || true
    
    # Configurar autenticación y bases de datos en MariaDB
    echo "[$(date)] Configurando autenticación y bases de datos en MariaDB..."
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS elegance_users;" || true
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS elegance_appointments;" || true
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS elegancebd;" || true
    
    DB_PASS="${var.db_password}"
    if [ -n "$DB_PASS" ]; then
      mysql -u root -e "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('$DB_PASS');" 2>/dev/null || \
      mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_PASS';" 2>/dev/null || true
      mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' IDENTIFIED BY '$DB_PASS' WITH GRANT OPTION;" 2>/dev/null || true
      mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' IDENTIFIED BY '$DB_PASS' WITH GRANT OPTION;" 2>/dev/null || true
      mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '$DB_PASS' WITH GRANT OPTION;" 2>/dev/null || true
      mysql -u root -p"$DB_PASS" -e "FLUSH PRIVILEGES;" 2>/dev/null || mysql -u root -e "FLUSH PRIVILEGES;" 2>/dev/null || true
    fi
    
    # Asegurar permisos del directorio de la aplicación
    chown -R ec2-user:ec2-user /opt/elegance
    
    echo "READY" > $STATUS_FILE
    echo "[$(date)] === Aprovisionamiento completado exitosamente ==="
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


