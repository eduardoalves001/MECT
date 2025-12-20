variable "backend_image" {
  description = "Backend Docker image"
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

variable "database_url" {
  description = "Database connection URL"
  type        = string
}

variable "cors_allowed_origin" {
  description = "CORS allowed origin"
  type        = string
}

variable "cors_allowed_origins" {
  description = "CORS allowed origins"
  type        = string
}

variable "cors_allowed_methods" {
  description = "CORS allowed methods"
  type        = string
}

variable "cors_allowed_headers" {
  description = "CORS allowed headers"
  type        = string
}

variable "cors_allow_credentials" {
  description = "CORS allow credentials"
  type        = string
}

variable "keycloak_issuer_uri" {
  description = "Keycloak issuer URI"
  type        = string
}

variable "keycloak_jwk_set_uri" {
  description = "Keycloak JWK set URI"
  type        = string
}

variable "flagsmith_api_url" {
  description = "Flagsmith API URL"
  type        = string
}

variable "flagsmith_environment_key" {
  description = "Flagsmith environment key"
  type        = string
  sensitive   = true
}

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

variable "otel_service_name" {
  description = "OpenTelemetry service name (OTEL_SERVICE_NAME)"
  type        = string
  default     = "rtmp-backend"
}

variable "otel_exporter_otlp_endpoint" {
  description = "OTLP exporter endpoint (OTEL_EXPORTER_OTLP_ENDPOINT)"
  type        = string
  default     = ""
}

variable "otel_exporter_otlp_headers" {
  description = "OTLP exporter headers (OTEL_EXPORTER_OTLP_HEADERS)"
  type        = string
  sensitive   = true
  default     = ""
}

variable "otel_exporter_otlp_protocol" {
  description = "OTLP exporter protocol (OTEL_EXPORTER_OTLP_PROTOCOL)"
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
  default     = "rtmp-backend-terraform"
}

variable "replica_count" {
  description = "Number of backend replicas"
  type        = number
  default     = 1
}
