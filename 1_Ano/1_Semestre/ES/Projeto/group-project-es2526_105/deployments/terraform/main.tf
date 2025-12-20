terraform {
  required_version = ">= 1.0"

  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  host = "unix:///var/run/docker.sock"
}

# Network and Volumes Module
module "network" {
  source = "./modules/network"

  network_name = var.docker_network_name
}

# Database Module
module "database" {
  source = "./modules/database"

  postgres_image    = var.postgres_image
  postgres_user     = var.postgres_user
  postgres_password = var.postgres_password
  postgres_db       = var.postgres_app_db
  restart_policy    = var.restart_policy
  network_name      = module.network.network_name
  volume_name       = module.network.volume_name
}

# Backend Module
module "backend" {
  source = "./modules/backend"

  backend_image             = var.backend_image
  postgres_user             = var.postgres_user
  postgres_password         = var.postgres_password
  database_url              = module.database.connection_string
  cors_allowed_origin       = var.frontend_url
  cors_allowed_origins      = var.cors_allowed_origins
  cors_allowed_methods      = var.cors_allowed_methods
  cors_allowed_headers      = var.cors_allowed_headers
  cors_allow_credentials    = var.cors_allow_credentials
  keycloak_issuer_uri       = var.keycloak_issuer_uri
  keycloak_jwk_set_uri      = var.keycloak_jwk_set_uri
  flagsmith_api_url         = var.flagsmith_api_url
  flagsmith_environment_key = var.flagsmith_environment_key
  google_ai_gemini_api_key   = var.google_ai_gemini_api_key
  google_ai_gemini_model_name = var.google_ai_gemini_model_name
  redis_host                = var.redis_host
  redis_port                = var.redis_port
  restart_policy            = var.restart_policy
  network_name              = module.network.network_name
  replica_count             = var.backend_replica_count

  # OpenTelemetry Configuration
  otel_service_name              = var.otel_service_name
  otel_exporter_otlp_endpoint    = var.otel_exporter_otlp_endpoint
  otel_exporter_otlp_headers     = var.otel_exporter_otlp_headers
  otel_exporter_otlp_protocol    = var.otel_exporter_otlp_protocol
  otel_metrics_exporter          = var.otel_metrics_exporter
  otel_logs_exporter             = var.otel_logs_exporter
  otel_traces_exporter           = var.otel_traces_exporter

  depends_on = [module.database]
}

# Frontend Module
module "frontend" {
  source = "./modules/frontend"

  frontend_image = var.frontend_image
  restart_policy = var.restart_policy
  network_name   = module.network.network_name
  replica_count  = var.frontend_replica_count

  depends_on = [module.backend]
}
