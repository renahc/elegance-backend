output "ec2_public_ip" {
  description = "IP pública de la instancia EC2 de Backend"
  value       = aws_instance.elegance_ec2.public_ip
}

output "ec2_username" {
  description = "Usuario SSH"
  value       = "ec2-user"
}

output "ec2_private_key_pem" {
  description = "Llave privada SSH (solo para el pipeline)"
  value       = tls_private_key.ec2_key.private_key_pem
  sensitive   = true
}

output "api_gateway_url" {
  description = "URL base del API Gateway HTTP para invocar microservicios"
  value       = aws_apigatewayv2_stage.default.invoke_url
}

output "rabbitmq_public_ip" {
  description = "Public IP of RabbitMQ instance"
  value       = aws_instance.rabbitmq.public_ip
}

output "backend_public_ip" {
  description = "Public IP of Backend instance"
  value       = aws_instance.backend.public_ip
}

output "rabbitmq_dashboard_url" {
  description = "URL del Dashboard de Gestión de RabbitMQ"
  value       = "http://${aws_instance.elegance_rabbitmq_ec2.public_ip}:15672"
}