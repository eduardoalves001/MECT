SHELL := /bin/bash

# Function to determine Docker Compose command
define docker_compose_cmd
	$(if $(shell command -v docker-compose 2> /dev/null),docker-compose,$(if $(shell command -v docker compose 2> /dev/null),docker compose,))
endef


prepare-env:
	cp .env-sample .env

up:
	$(call docker_compose_cmd) up

upd:
	$(call docker_compose_cmd) up -d
	@echo ""
	@echo "==================================="
	@echo "Services are running:"
	@echo "==================================="
	@echo "Frontend:  http://localhost:8090"
	@echo "Swagger:   http://localhost:8095/docs"
	@echo "pgAdmin:   http://localhost:8096"
	@echo "Keycloak:  http://localhost:8097"
	@echo "Keycloak:  http://localhost:8098"
	@echo "==================================="

down:
	$(call docker_compose_cmd) down

stop:
	$(call docker_compose_cmd) stop

build:
	$(call docker_compose_cmd) build

build-no-cache:
	$(call docker_compose_cmd) build --no-cache

logs:
	$(call docker_compose_cmd) logs -f

db-logs:
	$(call docker_compose_cmd) logs -f db

rm_volumes:
	docker volume rm group-project-es2526_105_db_data
	docker volume rm group-project-es2526_105_pgadmin_data

test_units:
	@echo "Running backend unit tests..."
	docker run --rm -v $(PWD)/backend:/app -w /app maven:3.9.8-eclipse-temurin-21-alpine mvn clean test jacoco:report
	@echo ""
	@echo "==================================="
	@echo "Unit Tests completed"
	@echo "==================================="
	@echo "Coverage Report: backend/target/site/jacoco/index.html"
	@echo "==================================="

test_e2e_docker:
	@echo "Running Cypress E2E tests in Docker..."
	@$(call docker_compose_cmd) --profile dev --profile test run --rm cypress
	@echo ""
	@echo "==================================="
	@echo "E2E Tests completed"
	@echo "==================================="
	@echo "Reports:     e2e/reports/"
	@echo "Screenshots: e2e/cypress/screenshots"
	@echo "Videos:      e2e/cypress/videos"
	@echo "==================================="

test_all: test_units test_e2e_docker

# Version Preview (read-only - actual releases happen via GitHub Actions)
version-current:
	@echo "Current version: $$(git describe --tags --abbrev=0 2>/dev/null || echo 'No tags yet')"

version-next-patch:
	@echo "Next patch release would be: $$(.github/scripts/bump-version.sh patch)"

version-next-minor:
	@echo "Next minor release would be: $$(.github/scripts/bump-version.sh minor)"

version-next-major:
	@echo "Next major release would be: $$(.github/scripts/bump-version.sh major)"