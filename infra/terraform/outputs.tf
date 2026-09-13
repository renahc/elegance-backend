output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "user_service_security_group_id" {
  value = aws_security_group.ecs_service.id
}

output "ecr_repository_url" {
  value = aws_ecr_repository.user_service.repository_url
}

output "ecr_repository_name" {
  value = aws_ecr_repository.user_service.name
}