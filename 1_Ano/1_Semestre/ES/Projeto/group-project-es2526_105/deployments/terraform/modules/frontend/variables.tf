variable "frontend_image" {
  description = "Frontend Docker image"
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

variable "container_name" {
  description = "Container name"
  type        = string
  default     = "rtmp-frontend-terraform"
}

variable "replica_count" {
  description = "Number of frontend replicas"
  type        = number
  default     = 1
}
