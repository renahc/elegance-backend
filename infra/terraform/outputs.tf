output "ec2_public_ip" {
  description = "IP pública de la instancia EC2"
  value       = aws_instance.elegance_ec2.public_ip
}

output "ec2_username" {
  description = "Usuario SSH de Amazon Linux 2023"
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

output "cognito_user_pool_id" {
  description = "ID del User Pool de Cognito"
  value       = aws_cognito_user_pool.elegance_pool.id
}

output "cognito_client_id" {
  description = "Client ID de la aplicación para Cognito"
  value       = aws_cognito_user_pool_client.elegance_client.id
}

output "cognito_issuer" {
  description = "URL del emisor de tokens JWT de Cognito"
  value       = "https://${aws_cognito_user_pool.elegance_pool.endpoint}"
}