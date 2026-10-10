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

  /*
  backend "s3" {
    bucket         = "elegance-tf-state-2026-gabriel"
    key            = "elegance/ec2/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "elegance-tf-lock"
  }
*/
}

provider "aws" {
  region = var.aws_region
}

# ==========================================
# LLAVE SSH COMPARTIDA
# ==========================================
resource "tls_private_key" "ec2_key" {
  algorithm = "RSA"
}

resource "aws_key_pair" "elegance_key" {
  key_name_prefix = "${var.app_name}-key-"
  public_key      = tls_private_key.ec2_key.public_key_openssh
}

# ==========================================
# SECURITY GROUP: EC2 BACKEND
# ==========================================
resource "aws_security_group" "elegance_sg" {
  name_prefix = "${var.app_name}-ec2-sg-"
  description = "Permite SSH y puertos de microservicios 8081-8084"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH"
  }

  ingress {
    from_port   = 8081
    to_port     = 8084
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Microservicios Spring Boot"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.app_name}-backend-sg"
  }
}

# ==========================================
# SECURITY GROUP: CLÚSTER RABBITMQ
# ==========================================
resource "aws_security_group" "rabbitmq_sg" {
  name_prefix = "${var.app_name}-rabbitmq-sg-"
  description = "Permite AMQP (5672/5673), Management UI (15672/15673) y SSH"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH"
  }

  ingress {
    from_port   = 5672
    to_port     = 5673
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "AMQP Node 1 y Node 2"
  }

  ingress {
    from_port   = 15672
    to_port     = 15673
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "RabbitMQ Management UI Web"
  }

  ingress {
    from_port   = 4369
    to_port     = 4369
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "EPMD Erlang"
  }

  ingress {
    from_port   = 25672
    to_port     = 25672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Erlang distribution"
  }

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.app_name}-rabbitmq-sg"
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
# INSTANCIA EC2 N° 1: BACKEND Y BD (Java 17 + MariaDB)
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
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS elegance_services;" || true
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
    Name = "elegance-backend-ec2"
  }
}

# ==========================================
# INSTANCIA EC2 N° 2: CLÚSTER RABBITMQ (2 NODOS DOCKER)
# ==========================================
resource "aws_instance" "elegance_rabbitmq_ec2" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.elegance_key.key_name
  security_groups             = [aws_security_group.rabbitmq_sg.name]
  user_data_replace_on_change = true

  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -e
    
    STATUS_FILE="/opt/rabbitmq/status.txt"
    mkdir -p /opt/rabbitmq
    chown -R ec2-user:ec2-user /opt/rabbitmq
    echo "BOOTING" > $STATUS_FILE

    trap 'echo "FAILED" > /opt/rabbitmq/status.txt' ERR
    exec > >(tee -a /var/log/user-data-rabbitmq.log) 2>&1

    echo "[$(date)] === Instalando Docker para RabbitMQ Clúster ==="
    amazon-linux-extras install -y docker || yum install -y docker
    systemctl enable docker
    systemctl start docker
    usermod -a -G docker ec2-user

    echo "[$(date)] Creando red Docker interna..."
    docker network create rabbitmq-cluster-net || true

    COOKIE="ELEGANCE_CLUSTER_SECRET_COOKIE_2026"

    # 1. Iniciar Nodo 1 (rabbitmq1)
    echo "[$(date)] Iniciando contenedor rabbitmq1..."
    docker run -d \
      --name rabbitmq1 \
      --hostname rabbitmq1 \
      --network rabbitmq-cluster-net \
      --restart unless-stopped \
      -p 5672:5672 \
      -p 15672:15672 \
      -e RABBITMQ_ERLANG_COOKIE=$COOKIE \
      -e RABBITMQ_DEFAULT_USER=admin \
      -e RABBITMQ_DEFAULT_PASS=admin123 \
      rabbitmq:3.13-management-alpine

    echo "[$(date)] Esperando que rabbitmq1 esté listo..."
    for i in $(seq 1 40); do
      if docker exec rabbitmq1 rabbitmqctl status >/dev/null 2>&1; then
        echo "[$(date)] rabbitmq1 listo."
        break
      fi
      sleep 2
    done

    # 2. Iniciar Nodo 2 (rabbitmq2)
    echo "[$(date)] Iniciando contenedor rabbitmq2..."
    docker run -d \
      --name rabbitmq2 \
      --hostname rabbitmq2 \
      --network rabbitmq-cluster-net \
      --restart unless-stopped \
      -p 5673:5672 \
      -p 15673:15672 \
      -e RABBITMQ_ERLANG_COOKIE=$COOKIE \
      rabbitmq:3.13-management-alpine

    echo "[$(date)] Esperando que rabbitmq2 esté listo para unirlo al clúster..."
    for i in $(seq 1 40); do
      if docker exec rabbitmq2 rabbitmqctl status >/dev/null 2>&1; then
        echo "[$(date)] Uniendo rabbitmq2 al clúster rabbit@rabbitmq1..."
        docker exec rabbitmq2 rabbitmqctl stop_app
        docker exec rabbitmq2 rabbitmqctl reset
        docker exec rabbitmq2 rabbitmqctl join_cluster rabbit@rabbitmq1
        docker exec rabbitmq2 rabbitmqctl start_app
        echo "[$(date)] rabbitmq2 unido exitosamente al clúster!"
        break
      fi
      sleep 2
    done

    echo "READY" > $STATUS_FILE
    echo "[$(date)] === Clúster RabbitMQ de 2 Nodos completamente activo y listo ==="
  EOF
  )

  tags = {
    Name = "elegance-rabbitmq-ec2"
  }
}

# ==========================================
# AWS API GATEWAY (HTTP API v2)
# ==========================================
resource "aws_apigatewayv2_api" "elegance_api" {
  name          = "${var.app_name}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = ["*"]
    allow_methods = ["GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"]
    allow_headers = ["Authorization", "Content-Type", "X-Amz-Date", "X-Api-Key", "X-Amz-Security-Token"]
    max_age       = 300
  }

  tags = {
    Name = "${var.app_name}-api-gateway"
  }
}

# ==========================================
# AZURE AD / ENTRA ID JWT AUTHORIZER
# ==========================================
resource "aws_apigatewayv2_authorizer" "azure_ad_auth" {
  api_id           = aws_apigatewayv2_api.elegance_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "${var.app_name}-azure-ad-authorizer"

  jwt_configuration {
    audience = [
      "413ae20f-d59e-4b81-8864-97d5945f7d5f",
      "api://304d54f7-d485-478a-a1ea-0c2f874b0c1f"
    ]
    issuer = "https://sts.windows.net/ff064edc-07f4-448c-97e1-49da14c085f5/"
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
# INTEGRACIONES HTTP PROXY (MICROSERVICIOS)
# ==========================================

# 1. Appointments (Puerto 8081)
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

# 2. Users: Clients, Stylists, Auth (Puerto 8082)
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

resource "aws_apigatewayv2_integration" "auth_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/auth"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "auth_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8082/api/v1/auth/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

# 3. Notifications (Puerto 8083)
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

# 4. Services & Rates (Puerto 8084)
resource "aws_apigatewayv2_integration" "services_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8084/api/v1/services"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "services_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8084/api/v1/services/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "rates_root_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8084/api/v1/rates"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

resource "aws_apigatewayv2_integration" "rates_proxy_int" {
  api_id                 = aws_apigatewayv2_api.elegance_api.id
  integration_type       = "HTTP_PROXY"
  integration_uri        = "http://${aws_instance.elegance_ec2.public_ip}:8084/api/v1/rates/{proxy}"
  integration_method     = "ANY"
  connection_type        = "INTERNET"
  payload_format_version = "1.0"
  depends_on             = [null_resource.wait_for_ec2]
}

# ==========================================
# RUTAS DE API GATEWAY (CON AZURE AD JWT AUTHORIZER)
# ==========================================

# Appointments
resource "aws_apigatewayv2_route" "appointments_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/appointments"
  target             = "integrations/${aws_apigatewayv2_integration.appointments_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "appointments_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/appointments/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.appointments_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Clients
resource "aws_apigatewayv2_route" "clients_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/clients"
  target             = "integrations/${aws_apigatewayv2_integration.clients_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "clients_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/clients/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.clients_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Stylists
resource "aws_apigatewayv2_route" "stylists_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/stylists"
  target             = "integrations/${aws_apigatewayv2_integration.stylists_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "stylists_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/stylists/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.stylists_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Auth
resource "aws_apigatewayv2_route" "auth_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/auth"
  target             = "integrations/${aws_apigatewayv2_integration.auth_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "auth_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/auth/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.auth_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Notifications
resource "aws_apigatewayv2_route" "notifications_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/notifications"
  target             = "integrations/${aws_apigatewayv2_integration.notifications_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "notifications_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/notifications/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.notifications_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Services
resource "aws_apigatewayv2_route" "services_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/services"
  target             = "integrations/${aws_apigatewayv2_integration.services_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "services_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/services/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.services_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# Rates
resource "aws_apigatewayv2_route" "rates_root_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/rates"
  target             = "integrations/${aws_apigatewayv2_integration.rates_root_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

resource "aws_apigatewayv2_route" "rates_route" {
  api_id             = aws_apigatewayv2_api.elegance_api.id
  route_key          = "ANY /api/v1/rates/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.rates_proxy_int.id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.azure_ad_auth.id
}

# ==========================================
# STAGE CON AUTO-DEPLOY
# ==========================================
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.elegance_api.id
  name        = "$default"
  auto_deploy = true

  tags = {
    Name = "${var.app_name}-default-stage"
  }
}
