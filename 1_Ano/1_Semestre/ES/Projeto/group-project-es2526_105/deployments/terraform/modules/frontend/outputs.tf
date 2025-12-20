output "container_ids" {
  description = "Frontend container IDs"
  value       = docker_container.frontend[*].id
}

output "container_names" {
  description = "Frontend container names"
  value       = docker_container.frontend[*].name
}

output "internal_hosts" {
  description = "Internal hostnames for frontend connections"
  value       = docker_container.frontend[*].name
}

output "internal_port" {
  description = "Internal port for frontend connections"
  value       = 80
}
