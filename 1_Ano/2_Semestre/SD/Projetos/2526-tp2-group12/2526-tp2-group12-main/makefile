
# Makefile updated with AI assistance
# ─────────────────────────────────────────────────────────────────────────────
# MOSS — Makefile
# ─────────────────────────────────────────────────────────────────────────────
# Usage examples:
#   make compile
#   make manager
#   make volume  VOLUME_PORT=4182 VOLUME_DIR=data/v2
#   make object
#   make gui
#   make put     FILE=./photo.jpg  REMOTE=photos/photo.jpg  BUCKET=sd
#   make get     REMOTE=photos/photo.jpg  DEST=/tmp/out.jpg  BUCKET=sd
#   make list    BUCKET=sd
#   make rm      REMOTE=photos/photo.jpg  BUCKET=sd
#   make up
#   make logs    SERVICE=object
# ─────────────────────────────────────────────────────────────────────────────

# ── Configurable defaults ────────────────────────────────────────────────────
MANAGER_PORT  ?= 4081
MANAGER_ADDR  ?= localhost:$(MANAGER_PORT)
MANAGER_DIR   ?= data/manager

VOLUME_PORT   ?= 4181
VOLUME_DIR    ?= data/volume

OBJECT_PORT   ?= 4281
OBJECT_DB     ?= data/obj.db

OBJ_ADDR      ?= localhost:$(OBJECT_PORT)
BUCKET        ?= sd
REFRESH       ?= 1
FILE          ?=
REMOTE        ?=
DEST          ?=

COMPOSE_FILE  := docker/compose.yml
SHELL         := /usr/bin/bash
RUN           := ./run

# ─────────────────────────────────────────────────────────────────────────────
# BUILD
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: compile
compile:          ## Compile the project
	@./mvnw compile -q

.PHONY: package
package:          ## Build the fat JAR (used by Docker)
	@./mvnw package -DskipTests -q

.PHONY: test
test:             ## Run all unit tests
	@./mvnw test

.PHONY: clean
clean:            ## Remove build artifacts
	@./mvnw clean -q

# ─────────────────────────────────────────────────────────────────────────────
# LOCAL NODES  (run directly from compiled classes via ./run)
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: manager
manager:          ## Start Manager node  [MANAGER_PORT=4081  MANAGER_DIR=data/manager]
	@mkdir -p $(MANAGER_DIR)
	$(RUN) manager -p $(MANAGER_PORT) -m $(MANAGER_DIR)

.PHONY: volume
volume:           ## Start a Volume node  [VOLUME_PORT=4181  VOLUME_DIR=data/volume  MANAGER_ADDR=localhost:4081]
	@mkdir -p $(VOLUME_DIR)
	$(RUN) volume -p $(VOLUME_PORT) -m $(MANAGER_ADDR) -d $(VOLUME_DIR)

.PHONY: object
object:           ## Start Object node   [OBJECT_PORT=4281  OBJECT_DB=data/obj.db  MANAGER_ADDR=localhost:4081]
	@mkdir -p $(dir $(OBJECT_DB))
	$(RUN) object -p $(OBJECT_PORT) -m $(MANAGER_ADDR) -d $(OBJECT_DB)

.PHONY: gui
gui:              ## Start GUI dashboard [MANAGER_ADDR=localhost:4081  OBJ_ADDR=localhost:4281  BUCKET=sd  REFRESH=3]
	$(RUN) gui -m $(MANAGER_ADDR) -o $(OBJ_ADDR) -b $(BUCKET) -r $(REFRESH)

# ─────────────────────────────────────────────────────────────────────────────
# CLI CLIENT
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: put
put:              ## Upload a file        FILE=<local>  REMOTE=<remote path>  [BUCKET=sd  OBJ_ADDR=localhost:4281]
ifndef FILE
	$(error FILE is required. Usage: make put FILE=./myfile.txt REMOTE=myfile.txt)
endif
ifndef REMOTE
	$(error REMOTE is required. Usage: make put FILE=./myfile.txt REMOTE=myfile.txt)
endif
	$(RUN) moss -r $(OBJ_ADDR) put -b $(BUCKET) $(FILE) $(REMOTE)

.PHONY: get
get:              ## Download a file      REMOTE=<remote path>  DEST=<local>  [BUCKET=sd  OBJ_ADDR=localhost:4281]
ifndef REMOTE
	$(error REMOTE is required. Usage: make get REMOTE=myfile.txt DEST=/tmp/myfile.txt)
endif
ifndef DEST
	$(error DEST is required. Usage: make get REMOTE=myfile.txt DEST=/tmp/myfile.txt)
endif
	$(RUN) moss -r $(OBJ_ADDR) get -b $(BUCKET) $(REMOTE) $(DEST)

.PHONY: list
list:             ## List objects in bucket  [BUCKET=sd  OBJ_ADDR=localhost:4281]
	$(RUN) moss -r $(OBJ_ADDR) list -b $(BUCKET)

.PHONY: rm
rm:               ## Delete an object         REMOTE=<remote path>  [BUCKET=sd  OBJ_ADDR=localhost:4281]
ifndef REMOTE
	$(error REMOTE is required. Usage: make rm REMOTE=myfile.txt)
endif
	$(RUN) moss -r $(OBJ_ADDR) rm -b $(BUCKET) $(REMOTE)

# ─────────────────────────────────────────────────────────────────────────────
# DOCKER COMPOSE
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: up
up: package       ## Build image and start full cluster via Docker Compose
	docker compose -f $(COMPOSE_FILE) up --build -d

.PHONY: down
down:             ## Stop and remove containers (keeps volumes)
	docker compose -f $(COMPOSE_FILE) down

.PHONY: restart
restart:          ## Restart all containers
	docker compose -f $(COMPOSE_FILE) restart

.PHONY: logs
logs:             ## Tail logs  [SERVICE=<name>  e.g. SERVICE=object]
	docker compose -f $(COMPOSE_FILE) logs -f $(SERVICE)

.PHONY: ps
ps:               ## Show running containers
	docker compose -f $(COMPOSE_FILE) ps

.PHONY: docker-clean
docker-clean:     ## Stop containers and DELETE all volumes (full reset)
	docker compose -f $(COMPOSE_FILE) down -v

# ─────────────────────────────────────────────────────────────────────────────
# HELP
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: help
help:             ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*##' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*##"}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
