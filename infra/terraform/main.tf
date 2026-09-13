terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  # NOTA: Para CI/CD real, descomenta y configura un backend S3 para guardar el estado
  # backend "s3" {
  #   bucket         = "tu-bucket-de-terraform-state"
  #   key            = "elegance/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region
}

# 0. Repositorio ECR para la imagen de User Service
resource "aws_ecr_repository" "user_service" {
  name                 = "${var.app_name}-user-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  # Permite hacer 'terraform destroy' aunque el repo tenga imágenes dentro
  # (útil en AWS Academy, donde vas a recrear todo seguido)
  force_delete = true
}

# 1. Usar la VPC y Subnets por defecto de AWS (para mantenerlo mínimo)
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# 2. Cluster ECS
resource "aws_ecs_cluster" "main" {
  name = "${var.app_name}-cluster"
}

# 3. Rol de ejecución para ECS (permite descargar la imagen de ECR)
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.app_name}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# 4. Definición de la Tarea (Task Definition)
resource "aws_ecs_task_definition" "user_service" {
  family                   = "${var.app_name}-user-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name  = "user-service"
      image = var.ecr_user_service_uri
      portMappings = [
        {
          containerPort = 8082
          hostPort      = 8082
        }
      ]
      environment = [
        { name = "SPRING_DATASOURCE_URL", value = var.db_url },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "AZURE_TENANT_ID", value = var.azure_tenant_id },
        { name = "AZURE_CLIENT_ID", value = var.azure_client_id },
        { name = "AZURE_CLIENT_SECRET", value = var.azure_client_secret }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/${var.app_name}-user-service"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# 5. Grupo de Seguridad para el Servicio
resource "aws_security_group" "ecs_service" {
  name   = "${var.app_name}-ecs-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    from_port   = 8082
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # En prod, restringir al ALB
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 6. Servicio ECS
resource "aws_ecs_service" "user_service" {
  name            = "${var.app_name}-user-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.user_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_service.id]
    assign_public_ip = true # Necesario si usas la VPC por defecto sin NAT Gateway
  }
}