terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

# Backend Image
resource "docker_image" "backend" {
  name         = var.backend_image
  keep_locally = false
  pull_triggers = [var.backend_image]
}

# Backend Container - Multiple replicas
resource "docker_container" "backend" {
  count = var.replica_count

  name  = "${var.container_name}-${count.index + 1}"
  image = docker_image.backend.image_id

  restart = var.restart_policy

  env = [
    "POSTGRES_USER=${var.postgres_user}",
    "POSTGRES_PASSWORD=${var.postgres_password}",
    "SPRING_DATASOURCE_URL=${var.database_url}",
    "CORS_ALLOWED_ORIGIN=${var.cors_allowed_origin}",
    "CORS_ALLOWED_ORIGINS=${var.cors_allowed_origins}",
    "CORS_ALLOWED_METHODS=${var.cors_allowed_methods}",
    "CORS_ALLOWED_HEADERS=${var.cors_allowed_headers}",
    "CORS_ALLOW_CREDENTIALS=${var.cors_allow_credentials}",
    "KEYCLOAK_ISSUER_URI=${var.keycloak_issuer_uri}",
    "KEYCLOAK_JWK_SET_URI=${var.keycloak_jwk_set_uri}",
    "FLAGSMITH_API_URL=${var.flagsmith_api_url}",
    "FLAGSMITH_ENVIRONMENT_KEY=${var.flagsmith_environment_key}",
    "GOOGLE_AI_GEMINI_API_KEY=${var.google_ai_gemini_api_key}",
    "GOOGLE_AI_GEMINI_MODEL_NAME=${var.google_ai_gemini_model_name}",
    "REDIS_HOST=${var.redis_host}",
    "REDIS_PORT=${var.redis_port}",
    "SPRING_PROFILES_ACTIVE=prod",
    "OTEL_SERVICE_NAME=${var.otel_service_name}-${count.index + 1}",
    "OTEL_EXPORTER_OTLP_ENDPOINT=${var.otel_exporter_otlp_endpoint}",
    "OTEL_EXPORTER_OTLP_HEADERS=${var.otel_exporter_otlp_headers}",
    "OTEL_EXPORTER_OTLP_PROTOCOL=${var.otel_exporter_otlp_protocol}",
    "OTEL_METRICS_EXPORTER=${var.otel_metrics_exporter}",
    "OTEL_LOGS_EXPORTER=${var.otel_logs_exporter}",
    "OTEL_TRACES_EXPORTER=${var.otel_traces_exporter}"
  ]

  ports {
    internal = 8080
  }

  healthcheck {
    test     = ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
    interval = "30s"
    timeout  = "10s"
    retries  = 3
  }

  networks_advanced {
    name = var.network_name
  }

  lifecycle {
    replace_triggered_by = [
      docker_image.backend
    ]
  }
}
