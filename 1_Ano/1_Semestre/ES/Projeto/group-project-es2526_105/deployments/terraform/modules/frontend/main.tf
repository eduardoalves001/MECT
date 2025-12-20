terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

# Frontend Image
resource "docker_image" "frontend" {
  name         = var.frontend_image
  keep_locally = false
  pull_triggers = [var.frontend_image]
}

# Frontend Container - Multiple replicas
resource "docker_container" "frontend" {
  count = var.replica_count
  
  name  = "${var.container_name}-${count.index + 1}"
  image = docker_image.frontend.image_id

  restart = var.restart_policy

  env = [
    "NODE_ENV=production"
  ]

  ports {
    internal = 80
  }

  networks_advanced {
    name = var.network_name
  }

  # Healthcheck to verify frontend is running
  healthcheck {
    test     = ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1"]
    interval = "10s"
    timeout  = "5s"
    retries  = 3
  }

  lifecycle {
    replace_triggered_by = [
      docker_image.frontend
    ]
  }
}
