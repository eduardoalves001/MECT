# =================================================================
# Terraform Variables - Production Deployment
# =================================================================
# This file contains PLACEHOLDERS that are automatically replaced
# by the GitHub Actions workflow during deployment.
# DO NOT commit this file with actual secrets!
# =================================================================

# Database Configuration
postgres_user     = "PLACEHOLDER_POSTGRES_USER"
postgres_password = "PLACEHOLDER_POSTGRES_PASSWORD"
postgres_app_db   = "PLACEHOLDER_POSTGRES_APP_DB"

# URLs and Domains
frontend_url = "PLACEHOLDER_FRONTEND_URL"
backend_url  = "PLACEHOLDER_BACKEND_URL"
keycloak_url = "PLACEHOLDER_KEYCLOAK_URL"
kc_hostname  = "PLACEHOLDER_KC_HOSTNAME"

# CORS Configuration
cors_allowed_origins   = "PLACEHOLDER_CORS_ALLOWED_ORIGINS"
cors_allowed_methods   = "GET,POST,PUT,DELETE,OPTIONS"
cors_allowed_headers   = "*"
cors_allow_credentials = "true"

# Keycloak Configuration
keycloak_issuer_uri  = "PLACEHOLDER_KEYCLOAK_ISSUER_URI"
keycloak_jwk_set_uri = "PLACEHOLDER_KEYCLOAK_JWK_SET_URI"

# Flagsmith Configuration
flagsmith_api_url         = "PLACEHOLDER_FLAGSMITH_API_URL"
flagsmith_environment_key = "PLACEHOLDER_FLAGSMITH_ENVIRONMENT_KEY"

# Google AI Gemini Configuration
google_ai_gemini_api_key   = "PLACEHOLDER_GOOGLE_AI_GEMINI_API_KEY"
google_ai_gemini_model_name = "PLACEHOLDER_GOOGLE_AI_GEMINI_MODEL_NAME"

# Docker Images (image tags replaced by workflow)
backend_image  = "ghcr.io/detiuaveiro/es2526-105-backend:PLACEHOLDER_BACKEND_IMAGE_TAG"
frontend_image = "ghcr.io/detiuaveiro/es2526-105-frontend:PLACEHOLDER_FRONTEND_IMAGE_TAG"

# Network Configuration
docker_network_name = "PLACEHOLDER_DOCKER_NETWORK_NAME"

# Container Restart Policy
restart_policy = "unless-stopped"

# OpenTelemetry & Elastic Observability Configuration
otel_service_name              = "rtmp-backend"
otel_exporter_otlp_endpoint    = "PLACEHOLDER_OTEL_EXPORTER_OTLP_ENDPOINT"
otel_exporter_otlp_headers     = "PLACEHOLDER_OTEL_EXPORTER_OTLP_HEADERS"
otel_exporter_otlp_protocol    = "http/protobuf"
otel_metrics_exporter          = "otlp"
otel_logs_exporter             = "otlp"
otel_traces_exporter           = "otlp"
