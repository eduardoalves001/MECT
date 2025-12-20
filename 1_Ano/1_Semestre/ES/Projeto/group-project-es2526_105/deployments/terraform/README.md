# Terraform Deployment for RTMP

This directory contains Terraform configuration files for deploying the Risk & Threat Modelling Platform (RTMP) using a modular approach.

## Architecture

The deployment is split into two parts:

### Infrastructure Services (Docker Compose)
- **Keycloak**: Authentication and authorization service
- **Flagsmith**: Feature flag management service  
- **Shared PostgreSQL DB**: Serves both Keycloak and Flagsmith
- **Nginx**: Reverse proxy for HTTPS termination and routing (serves entire platform)

### Application Services (Terraform)
- **Application PostgreSQL DB**: Dedicated database for RTMP application data
- **Backend**: Spring Boot REST API
- **Frontend**: React + Vite application

## Modular Structure

```
deployments/terraform/
├── main.tf                      # Root module configuration
├── variables.tf                 # Root input variables
├── outputs.tf                   # Root outputs
├── terraform.tfvars             # Configuration template (with placeholders)
├── .gitignore                   # Git ignore rules
│
├── modules/                     # Reusable Terraform modules
│   ├── network/                 # Network and volumes
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   │
│   ├── database/                # PostgreSQL database
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   │
│   ├── backend/                 # Spring Boot API
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   │
│   └── frontend/                # React frontend
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
│
└── docs/
    ├── README.md               # This file
    ├── ARCHITECTURE.md         # Detailed architecture
    └── QUICK_REFERENCE.md      # Quick commands reference
```

## Benefits of Modular Structure

### 1. **Reusability**
Each module can be reused across different environments or projects:
```hcl
module "backend_staging" {
  source = "./modules/backend"
  backend_image = "backend:staging"
  # ... other vars
}

module "backend_production" {
  source = "./modules/backend"
  backend_image = "backend:latest"
  # ... other vars
}
```

### 2. **Encapsulation**
Modules hide implementation details and expose clean interfaces:
- Input variables define what can be configured
- Outputs define what information is exposed
- Internal resources are hidden

### 3. **Maintainability**
Changes to a module are isolated and easier to test:
- Update database module without touching backend
- Modify nginx configuration independently
- Clear dependencies between modules

### 4. **Testability**
Each module can be tested independently:
```bash
cd modules/database
terraform init
terraform plan -var-file=test.tfvars
```

### 5. **Documentation**
Module structure serves as self-documentation:
- Clear separation of concerns
- Easy to understand dependencies
- Variables/outputs document interfaces

## Module Overview

### Network Module (`modules/network/`)
**Purpose**: Creates shared Docker network and volumes

**Inputs**:
- `network_name`: Name of the Docker network
- `network_driver`: Network driver (default: bridge)

**Outputs**:
- `network_name`: Network name for other modules
- `network_id`: Network ID
- `app_db_volume_name`: Volume name for app database

**Resources**:
- `docker_network.network`: Docker network for all containers
- `docker_volume.app_db_data`: Volume for application database

### Database Module (`modules/database/`)
**Purpose**: Deploys PostgreSQL database for application data

**Inputs**:
- `postgres_image`: PostgreSQL Docker image
- `postgres_user`: Database username
- `postgres_password`: Database password
- `postgres_db`: Database name
- `network_name`: Docker network to join
- `volume_name`: Volume for data persistence

**Outputs**:
- `container_id`: Container ID
- `container_name`: Container name
- `internal_host`: Hostname for internal connections

**Resources**:
- `docker_image.postgres`: PostgreSQL image
- `docker_container.database`: PostgreSQL container

### Backend Module (`modules/backend/`)
**Purpose**: Deploys Spring Boot REST API

**Inputs**:
- `backend_image`: Backend Docker image
- `postgres_*`: Database connection details
- `cors_*`: CORS configuration
- `keycloak_*`: Keycloak integration settings
- `flagsmith_*`: Feature flag settings
- `network_name`: Docker network to join

**Outputs**:
- `container_id`: Container ID
- `container_name`: Container name
- `internal_host`: Hostname for internal connections

**Resources**:
- `docker_image.backend`: Backend image
- `docker_container.backend`: Backend container

### Frontend Module (`modules/frontend/`)
**Purpose**: Deploys React + Vite frontend

**Inputs**:
- `frontend_image`: Frontend Docker image
- `network_name`: Docker network to join
- `restart_policy`: Container restart policy

**Outputs**:
- `container_id`: Container ID
- `container_name`: Container name
- `internal_host`: Hostname for internal connections

**Resources**:
- `docker_image.frontend`: Frontend image
- `docker_container.frontend`: Frontend container

## Prerequisites

- Terraform >= 1.0
- Docker and Docker Compose
- Access to GitHub Container Registry (GHCR) for pulling images
- Valid SSL certificates in `../../certs/` directory
- **Docker Compose network must exist** - Terraform references the existing Docker Compose network (`group-project-es2526_105_default`) for container communication

## Quick Start

### 1. Prepare Environment

First, ensure the infrastructure services are running:

```bash
# From project root
make prepare-env

# Start infrastructure services (this creates the Docker network)
docker compose -f docker-compose-prod.yaml up -d db flagsmith keycloak-prod

# Wait for services to be healthy
./scripts/wait-for-healthy.sh db rtmp-keycloak-prod rtmp-flagsmith
```

**Important**: The Docker Compose command above creates the network `group-project-es2526_105_default` that Terraform will use to attach its containers.

### 2. Create Variables File

Copy the example variables file and update with your values:

```bash
cd deployments/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your configuration:

```hcl
postgres_user     = "your-db-user"
postgres_password = "your-secure-password"
flagsmith_environment_key = "your-flagsmith-key"
# ... other variables
```

### 3. Deploy with Terraform

```bash
# Initialize Terraform (downloads providers and modules)
terraform init

# Preview changes
terraform plan

# Apply changes
terraform apply
```

Note: Nginx is managed by Docker Compose, not Terraform.

### 4. Verify Deployment

```bash
# Check container status
docker ps | grep rtmp

# Test backend health
curl -k https://deti-engsoft-05.ua.pt/api/actuator/health

# Access application
open https://deti-engsoft-05.ua.pt
```

## Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `postgres_user` | PostgreSQL username | `user` |
| `postgres_password` | PostgreSQL password | `securepass123` |
| `flagsmith_environment_key` | Flagsmith environment key | `ser.xxx` |

### Optional Variables (with defaults)

| Variable | Default | Description |
|----------|---------|-------------|
| `backend_image` | `ghcr.io/detiuaveiro/es2526-105-backend:2.1.0` | Backend Docker image |
| `frontend_image` | `ghcr.io/detiuaveiro/es2526-105-frontend:2.1.0` | Frontend Docker image |
| `postgres_image` | `postgres:17-alpine` | PostgreSQL image |
| `frontend_url` | `https://deti-engsoft-05.ua.pt` | Frontend URL |
| `docker_network_name` | `rtmp-network` | Docker network name |

See `variables.tf` for complete list.

## Outputs

After successful deployment, Terraform outputs:

- `network_name`: Docker network name
- `app_database_container_id`: Application database container ID
- `backend_container_id`: Backend container ID
- `frontend_container_id`: Frontend container ID
- `application_url`: Full application URL
- `api_url`: API endpoint URL

View outputs:

```bash
terraform output
```

## Module Dependencies

The root configuration establishes dependencies between modules:

```
network (standalone)
   ↓
database (depends on: network)
   ↓
backend (depends on: database, network)
   ↓
frontend (depends on: backend, network)
   ↓
nginx (depends on: frontend, backend, network)
```

Terraform automatically handles the creation order based on these dependencies.

## Advanced Usage

### Using Individual Modules

You can use individual modules in your own configurations:

```hcl
module "my_database" {
  source = "./modules/database"

  postgres_image    = "postgres:17-alpine"
  postgres_user     = "myuser"
  postgres_password = var.db_password
  postgres_db       = "mydb"
  network_name      = "my-network"
  volume_name       = "my-volume"
  restart_policy    = "always"
}
```

### Testing Modules

Test individual modules:

```bash
# Create test directory
mkdir -p test/database
cd test/database

# Create test configuration
cat > main.tf <<EOF
module "database_test" {
  source = "../../modules/database"
  
  postgres_image    = "postgres:17-alpine"
  postgres_user     = "testuser"
  postgres_password = "testpass"
  postgres_db       = "testdb"
  network_name      = "test-network"
  volume_name       = "test-volume"
  restart_policy    = "no"
}
EOF

# Test
terraform init
terraform plan
terraform apply
terraform destroy
```

### Module Versioning

For production use, consider versioning modules:

```hcl
module "backend" {
  source = "git::https://github.com/yourorg/terraform-modules.git//backend?ref=v1.0.0"
  # ... variables
}
```

## Database Strategy

**Two separate PostgreSQL instances:**

1. **Shared DB (Docker Compose)**: Port 5432
   - Serves Keycloak (`keycloak` database)
   - Serves Flagsmith (`flagsmith` database)
   
2. **App DB (Terraform)**: Port 5433
   - Serves RTMP application (`db` database)
   - Managed by Terraform
   - Data persisted in Docker volume

This separation ensures:
- Clean resource management boundaries
- Infrastructure services (Keycloak/Flagsmith) remain stable
- Application data can be managed independently

## Troubleshooting

### Module Issues

**Problem**: Module not found
```
Error: Module not installed
```

**Solution**: Run `terraform init` to download modules

---

**Problem**: Module outputs not available
```
Error: Reference to undeclared output value
```

**Solution**: Check module's `outputs.tf` for available outputs

### Container Health Issues

```bash
# Check container logs
docker logs rtmp-backend-terraform
docker logs rtmp-frontend-terraform
docker logs rtmp-app-db

# Check container health
docker inspect rtmp-backend-terraform | jq '.[0].State.Health'
```

### Network Issues

```bash
# Verify network exists
docker network ls | grep rtmp

# Inspect network
docker network inspect rtmp-network

# Check which containers are connected
docker network inspect rtmp-network -f '{{range .Containers}}{{.Name}} {{end}}'
```

### State Issues

```bash
# Show current state
terraform state list

# Show specific resource
terraform state show module.backend.docker_container.backend

# Remove resource from state (if needed)
terraform state rm module.backend.docker_container.backend
```

## CI/CD Integration

This Terraform configuration is used by the `deploy_terraform.yml` GitHub Actions workflow:

1. Workflow starts infrastructure services with Docker Compose
2. Generates `terraform.tfvars` from GitHub secrets
3. Runs `terraform init`, `plan`, and `apply`
4. Verifies service health
5. Reports deployment status

See `.github/workflows/deploy_terraform.yml` for implementation details.

## Security Notes

- **Sensitive Variables**: Mark as `sensitive = true` in `variables.tf`
- **tfvars File**: Never commit `terraform.tfvars` with secrets
- **State File**: Contains sensitive data - protect `terraform.tfstate`
- **SSL Certificates**: Ensure valid certs in `../../certs/` directory
- **Module Sources**: Use trusted sources for external modules

## Migration from Flat Structure

If you have an existing flat Terraform structure:

1. Backup your state: `terraform state pull > backup.tfstate`
2. Review the new modular structure
3. Update your `terraform.tfvars` (variable names are the same)
4. Run `terraform init` to initialize modules
5. Run `terraform plan` to preview changes
6. If resources are being destroyed/recreated, use `terraform state mv` to migrate state

## Best Practices

1. **Version Control**: Always commit module changes
2. **Documentation**: Update module README when changing interfaces
3. **Variables**: Use meaningful names and descriptions
4. **Outputs**: Export only necessary information
5. **Dependencies**: Use explicit `depends_on` when needed
6. **Testing**: Test modules independently before integration
7. **State Management**: Consider remote state for production
8. **Secrets**: Use Terraform Cloud or environment variables for secrets

## References

- [Terraform Docker Provider](https://registry.terraform.io/providers/kreuzwerker/docker/latest/docs)
- [Terraform Module Documentation](https://www.terraform.io/language/modules)
- [Project Documentation](../../docs/)
- [Architecture Details](./ARCHITECTURE.md)
- [Quick Reference](./QUICK_REFERENCE.md)
- [Docker Compose Configuration](../../docker-compose-prod.yaml)
