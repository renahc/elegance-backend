output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "user_service_security_group_id" {
  value = aws_security_group.ecs_service.id
}