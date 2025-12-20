terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

# PostgreSQL Image
resource "docker_image" "postgres" {
  name = var.postgres_image
}

# Application Database Container
resource "docker_container" "app_db" {
  name  = var.container_name
  image = docker_image.postgres.image_id

  restart = var.restart_policy

  env = [
    "POSTGRES_USER=${var.postgres_user}",
    "POSTGRES_PASSWORD=${var.postgres_password}",
    "POSTGRES_DB=${var.postgres_db}"
  ]

  ports {
    internal = 5432
    external = var.external_port
  }

  volumes {
    volume_name    = var.volume_name
    container_path = "/var/lib/postgresql/data"
  }

  healthcheck {
    test     = ["CMD-SHELL", "pg_isready -U ${var.postgres_user} -d ${var.postgres_db} -h 127.0.0.1"]
    interval = "10s"
    timeout  = "5s"
    retries  = 5
  }

  networks_advanced {
    name = var.network_name
  }
}
