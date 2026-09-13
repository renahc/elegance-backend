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