output "container_id" {
  description = "Database container ID"
  value       = docker_container.app_db.id
}

output "container_name" {
  description = "Database container name"
  value       = docker_container.app_db.name
}

output "internal_host" {
  description = "Internal hostname for database connections"
  value       = var.container_name
}

output "internal_port" {
  description = "Internal port for database connections"
  value       = 5432
}

output "connection_string" {
  description = "JDBC connection string"
  value       = "jdbc:postgresql://${var.container_name}:5432/${var.postgres_db}"
  sensitive   = true
}
