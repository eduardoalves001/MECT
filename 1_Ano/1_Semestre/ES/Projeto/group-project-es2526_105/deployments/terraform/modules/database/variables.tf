variable "postgres_image" {
  description = "PostgreSQL Docker image"
  type        = string
}

variable "postgres_user" {
  description = "PostgreSQL username"
  type        = string
  sensitive   = true
}

variable "postgres_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "postgres_db" {
  description = "Database name"
  type        = string
}

variable "restart_policy" {
  description = "Container restart policy"
  type        = string
}

variable "network_name" {
  description = "Docker network name"
  type        = string
}

variable "volume_name" {
  description = "Docker volume name"
  type        = string
}

variable "container_name" {
  description = "Container name"
  type        = string
  default     = "rtmp-app-db"
}

variable "external_port" {
  description = "External port for database"
  type        = number
  default     = 5433
}
