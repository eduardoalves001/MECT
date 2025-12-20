# ========================================
# Network Outputs
# ========================================

output "network_name" {
  description = "Docker network name"
  value       = module.network.network_name
}

output "network_id" {
  description = "Docker network ID"
  value       = module.network.network_id
}

# ========================================
# Database Outputs
# ========================================

output "app_database_container_id" {
  description = "Application database container ID"
  value       = module.database.container_id
}

output "app_database_container_name" {
  description = "Application database container name"
  value       = module.database.container_name
}

output "database_internal_host" {
  description = "Database internal hostname"
  value       = module.database.internal_host
}

# ========================================
# Backend Outputs
# ========================================

output "backend_container_ids" {
  description = "Backend container IDs"
  value       = module.backend.container_ids
}

output "backend_container_names" {
  description = "Backend container names"
  value       = module.backend.container_names
}

output "backend_internal_hosts" {
  description = "Backend internal hostnames"
  value       = module.backend.internal_hosts
}

# ========================================
# Frontend Outputs
# ========================================

output "frontend_container_ids" {
  description = "Frontend container IDs"
  value       = module.frontend.container_ids
}

output "frontend_container_names" {
  description = "Frontend container names"
  value       = module.frontend.container_names
}

output "frontend_internal_hosts" {
  description = "Frontend internal hostnames"
  value       = module.frontend.internal_hosts
}

# ========================================
# Application URLs
# ========================================

output "application_url" {
  description = "Application URL"
  value       = var.frontend_url
}

output "api_url" {
  description = "API URL"
  value       = "${var.backend_url}/api"
}
