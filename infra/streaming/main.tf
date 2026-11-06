terraform {
  required_version = ">= 1.9.0"
  required_providers { aws = { source = "hashicorp/aws", version = "~> 5.0" } }
}
provider "aws" {
  region = var.aws_region
}
variable "aws_region" {
  type    = string
  default = "us-east-1"
}
variable "instance_type" {
  type    = string
  default = "t3.large"
}
variable "key_name" {
  type = string
}
variable "admin_cidr" {
  type = string
  validation {
    condition     = var.admin_cidr != "0.0.0.0/0" && can(cidrnetmask(var.admin_cidr))
    error_message = "Use a restricted administrator CIDR."
  }
}
variable "volume_size" {
  type    = number
  default = 60
}
data "aws_vpc" "default" {
  default = true
}
data "aws_ssm_parameter" "ami" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id"
}
resource "aws_security_group" "streaming" {
  name_prefix = "elegance-streaming-"
  vpc_id      = data.aws_vpc.default.id
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
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
resource "aws_instance" "streaming" {
  ami                         = data.aws_ssm_parameter.ami.value
  instance_type               = var.instance_type
  key_name                    = var.key_name
  vpc_security_group_ids      = [aws_security_group.streaming.id]
  associate_public_ip_address = true
  metadata_options {
    http_tokens = "required"
  }
  root_block_device {
    volume_type = "gp3"
    volume_size = var.volume_size
    encrypted   = true
  }
  user_data = <<-BASH
 #!/bin/bash
 set -euo pipefail
 apt-get update
 apt-get install -y docker.io docker-compose-v2
 systemctl enable --now docker
 usermod -aG docker ubuntu
 install -d -o ubuntu -g ubuntu /opt/elegance /opt/elegance/public
 BASH
  tags = {
  Name = "elegance-streaming-ec2" }
}
output "host" {
  value = aws_instance.streaming.public_ip
}
output "ssh_user" {
  value = "ubuntu"
}
output "dashboard_tunnel" {
  value = "ssh -L 8090:127.0.0.1:8090 -L 15672:127.0.0.1:15672 ubuntu@${aws_instance.streaming.public_ip}"
}
