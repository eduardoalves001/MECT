output "network_name" {
  description = "Docker network name"
  value       = data.docker_network.network.name
}

output "network_id" {
  description = "Docker network ID"
  value       = data.docker_network.network.id
}

output "volume_name" {
  description = "Database volume name"
  value       = docker_volume.app_db_data.name
}
