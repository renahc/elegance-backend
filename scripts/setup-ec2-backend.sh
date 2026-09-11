#!/bin/bash
# AWS EC2 Spring Boot Backend Provisioning Script
set -e

echo "=== Updating system and installing OpenJDK 17 ==="
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless mysql-client

echo "=== Creating backend deployment directory ==="
sudo mkdir -p /opt/salon-backend
sudo chown -R ubuntu:ubuntu /opt/salon-backend

echo "=== Installing systemd service ==="
sudo tee /etc/systemd/system/salon-backend.service << 'EOF'
[Unit]
Description=Élégance Salon Spring Boot Backend REST API Service
After=syslog.target network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/opt/salon-backend
ExecStart=/usr/bin/java -jar /opt/salon-backend/salon-backend-1.0.0.jar
SuccessExitStatus=143
TimeoutStopSec=20
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable salon-backend

echo "=== Backend EC2 Setup Complete! ==="
