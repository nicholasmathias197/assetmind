# ==================================================
# Terraform Configuration for AssetMind
# ==================================================
# This Terraform configuration deploys the complete infrastructure for the
# AssetMind application, including:
# - EC2 instance running Spring Boot backend with MySQL
# - S3 bucket hosting React frontend as a static website
# - Security groups, IAM roles, and monitoring
# ==================================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend configuration for storing Terraform state in S3
  backend "s3" {
    bucket = "assetmind-frontend-911784620581"
    key    = "tfstate"
    region = "us-east-2"
  }
}

# ==================================================
# AWS Provider Configuration
# ==================================================
provider "aws" {
  region = "us-east-2"
}

# ==================================================
# Data Sources
# ==================================================
data "aws_caller_identity" "current" {}

# ==================================================
# Security Group Configuration
# ==================================================
resource "aws_security_group" "spring_boot_sg" {
  name        = "assetmind-sg"
  description = "Allow HTTP, app, and SSH traffic"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "assetmind-sg"
    Environment = "production"
    Project     = "assetmind"
  }
}

# ==================================================
# IAM Roles and Policies
# ==================================================
resource "aws_iam_role" "ec2_role" {
  name = "assetmind-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_s3" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "assetmind-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ==================================================
# EC2 Instance for Spring Boot Backend
# ==================================================
resource "aws_instance" "spring_boot_app" {
  ami                    = "ami-0b0b78dcacbab728f"  # Amazon Linux 2023 (us-east-2)
  instance_type          = "t3.micro"
  key_name               = "assetmind-key"
  vpc_security_group_ids = [aws_security_group.spring_boot_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data_replace_on_change = true

  user_data = <<-EOF
#!/bin/bash
exec > /var/log/user-data.log 2>&1

# Install Java 21 (Corretto distribution)
dnf install -y java-21-amazon-corretto-headless

# Install MySQL server
dnf install -y mariadb105-server

# Install AWS Systems Manager agent
dnf install -y amazon-ssm-agent
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

# Configure and start MySQL
systemctl enable mariadb
systemctl start mariadb
sleep 5

# Set MySQL root password and create application database
mysql <<'SQLEOF'
ALTER USER 'root'@'localhost' IDENTIFIED VIA mysql_native_password USING PASSWORD('Postgres1');
FLUSH PRIVILEGES;
CREATE DATABASE IF NOT EXISTS assetmind;
SQLEOF

# Create application directory
mkdir -p /opt/assetmind

# Download Spring Boot application JAR from S3
aws s3 cp s3://assetmind-frontend-911784620581/app/assetmind-application.jar /opt/assetmind/assetmind-application.jar

# Create systemd service for automatic startup and management
# AssetMind uses Flyway for DB migrations — the app auto-migrates on startup
cat > /etc/systemd/system/assetmind.service <<SERVICE
[Unit]
Description=AssetMind Spring Boot App
After=network.target mariadb.service

[Service]
ExecStart=/usr/bin/java -jar /opt/assetmind/assetmind-application.jar
Environment=ASSETMIND_DB_URL=jdbc:mysql://localhost:3306/assetmind
Environment=ASSETMIND_DB_USERNAME=root
Environment=ASSETMIND_DB_PASSWORD=Postgres1
Environment=JWT_SECRET=CHANGE-ME-in-production-use-at-least-64-chars-for-hs512-2026
Environment=BOOTSTRAP_SECRET=CHANGE-ME-in-production-use-a-strong-random-value
Restart=always
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
SERVICE

# Enable and start the application service
systemctl daemon-reload
systemctl enable assetmind
systemctl start assetmind
  EOF

  tags = {
    Name        = "assetmind-backend"
    Environment = "production"
    Project     = "assetmind"
  }
}

# ==================================================
# SNS Topics for Monitoring Alerts
# ==================================================
resource "aws_sns_topic" "alerts" {
  name = "assetmind-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = "nicholas.mathias@peopleshores.com"
}

# ==================================================
# CloudWatch Monitoring Alarms
# ==================================================
resource "aws_cloudwatch_metric_alarm" "ec2_cpu_high" {
  alarm_name          = "assetmind-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_description   = "EC2 CPU utilization exceeded 80% for 10 minutes"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.spring_boot_app.id
  }

  tags = {
    Name        = "assetmind-cpu-alarm"
    Environment = "production"
    Project     = "assetmind"
  }
}

resource "aws_cloudwatch_metric_alarm" "ec2_status_check" {
  alarm_name          = "assetmind-status-check"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_description   = "EC2 instance failed status check"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.spring_boot_app.id
  }

  tags = {
    Name        = "assetmind-status-alarm"
    Environment = "production"
    Project     = "assetmind"
  }
}

# ==================================================
# AWS Budget Alert
# ==================================================
resource "aws_budgets_budget" "monthly" {
  name         = "assetmind-monthly-budget"
  budget_type  = "COST"
  limit_amount = "20"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = ["nicholas.mathias@peopleshores.com"]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = ["nicholas.mathias@peopleshores.com"]
  }
}

# ==================================================
# S3 Bucket for React Frontend Hosting
# ==================================================
resource "aws_s3_bucket" "react_frontend" {
  bucket = "assetmind-frontend-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name        = "assetmind-frontend"
    Environment = "production"
    Project     = "assetmind"
  }
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.react_frontend.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"  # SPA routing — redirect errors to index
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.react_frontend.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket     = aws_s3_bucket.react_frontend.id
  depends_on = [aws_s3_bucket_public_access_block.frontend]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.react_frontend.arn}/*"
    }]
  })
}
