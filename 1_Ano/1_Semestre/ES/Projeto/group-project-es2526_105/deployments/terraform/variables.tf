# Docker images
variable "backend_image" {
  description = "Backend Docker image"
  type        = string
  default     = "ghcr.io/detiuaveiro/es2526-105-backend:3.0.0"
}

variable "frontend_image" {
  description = "Frontend Docker image"
  type        = string
  default     = "ghcr.io/detiuaveiro/es2526-105-frontend:3.0.0"
}

variable "postgres_image" {
  description = "PostgreSQL Docker image"
  type        = string
  default     = "postgres:17-alpine"
}

# Database configuration
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

variable "postgres_app_db" {
  description = "Application database name"
  type        = string
  default     = "db"
}

# URLs and domains
variable "frontend_url" {
  description = "Frontend URL"
  type        = string
  default     = "https://deti-engsoft-05.ua.pt"
}

variable "backend_url" {
  description = "Backend URL"
  type        = string
  default     = "https://deti-engsoft-05.ua.pt"
}

variable "keycloak_url" {
  description = "Keycloak URL"
  type        = string
  default     = "https://deti-engsoft-05.ua.pt/auth"
}

variable "kc_hostname" {
  description = "Keycloak hostname"
  type        = string
  default     = "deti-engsoft-05.ua.pt"
}

# CORS configuration
variable "cors_allowed_origins" {
  description = "CORS allowed origins"
  type        = string
  default     = "https://deti-engsoft-05.ua.pt"
}

variable "cors_allowed_methods" {
  description = "CORS allowed methods"
  type        = string
  default     = "GET,POST,PUT,DELETE,OPTIONS"
}

variable "cors_allowed_headers" {
  description = "CORS allowed headers"
  type        = string
  default     = "*"
}

variable "cors_allow_credentials" {
  description = "CORS allow credentials"
  type        = string
  default     = "true"
}

# Keycloak configuration
variable "keycloak_issuer_uri" {
  description = "Keycloak issuer URI"
  type        = string
  default     = "https://deti-engsoft-05.ua.pt/auth/realms/rtmp"
}

variable "keycloak_jwk_set_uri" {
  description = "Keycloak JWK set URI (internal)"
  type        = string
  default     = "http://keycloak-prod:8080/auth/realms/rtmp/protocol/openid-connect/certs"
}

# Flagsmith configuration
variable "flagsmith_api_url" {
  description = "Flagsmith API URL (internal)"
  type        = string
  default     = "http://flagsmith:8000/api/v1/"
}

variable "flagsmith_environment_key" {
  description = "Flagsmith environment key"
  type        = string
  sensitive   = true
}

# Google AI Gemini Configuration
variable "google_ai_gemini_api_key" {
  description = "Google AI Gemini API Key"
  type        = string
  sensitive   = true
}

variable "google_ai_gemini_model_name" {
  description = "Google AI Gemini Model Name"
  type        = string
  default     = "gemini-2.5-flash"
}

# Redis configuration
variable "redis_host" {
  description = "Redis host"
  type        = string
  default     = "redis"
}

variable "redis_port" {
  description = "Redis port"
  type        = string
  default     = "6379"
}

# Network configuration
variable "docker_network_name" {
  description = "Docker network name"
  type        = string
  default     = "rtmp-network"
}

# Container restart policy
variable "restart_policy" {
  description = "Container restart policy"
  type        = string
  default     = "unless-stopped"
}

# Replica counts
variable "backend_replica_count" {
  description = "Number of backend replicas"
  type        = number
  default     = 2
}

variable "frontend_replica_count" {
  description = "Number of frontend replicas"
  type        = number
  default     = 2
}

# OpenTelemetry & Elastic Observability Configuration
variable "otel_service_name" {
  description = "OpenTelemetry service name"
  type        = string
  default     = "rtmp-backend"
}

variable "otel_exporter_otlp_endpoint" {
  description = "OTLP exporter endpoint (Elastic APM URL)"
  type        = string
  default     = ""
}

variable "otel_exporter_otlp_headers" {
  description = "OTLP exporter headers (Authorization header with Elastic secret token)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "otel_exporter_otlp_protocol" {
  description = "OTLP exporter protocol"
  type        = string
  default     = "http/protobuf"
}

variable "otel_metrics_exporter" {
  description = "Metrics exporter selection"
  type        = string
  default     = "otlp"
}

variable "otel_logs_exporter" {
  description = "Logs exporter selection"
  type        = string
  default     = "otlp"
}

variable "otel_traces_exporter" {
  description = "Traces exporter selection"
  type        = string
  default     = "otlp"
}
