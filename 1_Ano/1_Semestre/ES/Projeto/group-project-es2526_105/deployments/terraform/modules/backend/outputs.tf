output "container_ids" {
  description = "Backend container IDs"
  value       = docker_container.backend[*].id
}

output "container_names" {
  description = "Backend container names"
  value       = docker_container.backend[*].name
}

output "internal_hosts" {
  description = "Internal hostnames for backend connections"
  value       = docker_container.backend[*].name
}

output "internal_port" {
  description = "Internal port for backend connections"
  value       = 8080
}
