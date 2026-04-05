output "ec2_public_ip" {
  description = "Public IP of EC2 instance"
  value       = aws_instance.spring_boot_app.public_ip
}

output "s3_bucket_url" {
  description = "S3 website URL"
  value       = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

output "sns_topic_arn" {
  description = "SNS topic ARN for CloudWatch alerts"
  value       = aws_sns_topic.alerts.arn
}
