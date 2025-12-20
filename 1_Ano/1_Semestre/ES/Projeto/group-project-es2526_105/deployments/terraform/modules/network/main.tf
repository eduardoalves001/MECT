terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

# Reference existing Docker Compose network
data "docker_network" "network" {
  name = var.network_name
}

# Volume for application database
resource "docker_volume" "app_db_data" {
  name = "rtmp_app_db_data"
}
