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
    exec > >(tee /var/log/user-data.log | logger -t user-data -s 2>/dev/console) 2>&1

    echo "=== Iniciando instalación de dependencias en AL2023 ==="
    dnf update -y
    dnf install -y java-17-amazon-corretto mariadb105-server
    
    echo "=== Iniciando y habilitando servicio MariaDB ==="
    systemctl enable --now mariadb
    sleep 5
    
    echo "=== Configurando usuario root y bases de datos ==="
    mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${var.db_password}'; FLUSH PRIVILEGES;"
    mysql -u root -p'${var.db_password}' -e "CREATE DATABASE IF NOT EXISTS elegance_users; CREATE DATABASE IF NOT EXISTS elegance_appointments;"
    
    echo "=== Creando directorio de la aplicación ==="
    mkdir -p /opt/elegance
    chown -R ec2-user:ec2-user /opt/elegance
    echo "READY" > /opt/elegance/status.txt
    echo "=== user_data completado exitosamente ==="
  EOF

  tags = {
    Name = "${var.app_name}-ec2"
  }
}