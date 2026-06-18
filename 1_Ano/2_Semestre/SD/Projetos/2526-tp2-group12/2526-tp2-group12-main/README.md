# MOSS - Microservice-oriented Object Storage Service

MOSS is a distributed object storage system designed with a microservices architecture.
It provides a scalable solution for storing and retrieving binary objects over a network using gRPC.

## Architecture

The system consists of several specialized node types:

- **Manager Node:** The central coordinator. It maintains a registry of available Volume Nodes and manages file placement.
- **Volume Node:** The storage layer. These nodes store raw data on the local file system and provide read/write access. They send periodic heartbeats to the Manager.
- **Object Node:** The abstraction layer. It provides an object-level API (Put/Get/List) to clients, coordinating with the Manager to find volumes and with Volumes to transfer data.
- **Client CLI:** A command-line tool for users to interact with MOSS.

## Features

- **Distributed Storage:** Data is spread across multiple volume nodes.
- **Heartbeat Mechanism:** Manager tracks node health via periodic heartbeats (every 5s, 20s stale timeout).
- **gRPC Communication:** High-performance communication between nodes.
- **Persistent Metadata:** Manager ledger and Object database survive restarts.
- **Docker Support:** Easy deployment using Docker Compose.

---

## Prerequisites

- **Java 21** or higher (must be set in `PATH` / `JAVA_HOME` before running any command).
- **Docker & Docker Compose** (for containerised deployment).

---

## Option 1 — Docker Compose (Recommended)

Starts a full cluster (1 Manager, 2 Volume Nodes, 1 Object Node) in containers.

```bash
make up        # build image and start all containers
make ps        # verify they are running
make logs      # tail all logs  (Ctrl+C to stop)
make logs SERVICE=object   # tail a single service
make down          # stop containers (data is kept)
make docker-clean  # stop containers and delete all data
```

The Object Node is exposed on port `4281`. Wait ~10 seconds for Volume nodes to register before uploading.

---

## Option 2 — Manual Setup

Compile first, then start each node in a separate terminal.

### Build

```bash
make compile
# or
./mvnw compile
```

### Start nodes

```bash
# Terminal 1 — Manager
make manager
# or: ./run manager -p 4081 -m data/manager

# Terminal 2 — Volume node 1
make volume PORT=4181 DIR=data/v1
# or: ./run volume -p 4181 -m localhost:4081 -d data/v1

# Terminal 3 — Volume node 2
make volume PORT=4182 DIR=data/v2
# or: ./run volume -p 4182 -m localhost:4081 -d data/v2

# Terminal 4 — Object node
make object
# or: ./run object -p 4281 -m localhost:4081
```

> Wait ~10 seconds after starting the Volume nodes. The Manager requires heartbeats from at least 2 Volume nodes before accepting assignments.

---

## Client Usage

All commands default to bucket `sd` and Object node at `localhost:4281`. Override with `BUCKET=` and `OBJ_ADDR=`.

### Upload a file

```bash
make put FILE=assets/hello.txt REMOTE=hello.txt
make put FILE=assets/photo.png REMOTE=photos/photo.png
make put FILE=assets/assignment.pdf REMOTE=docs/assignment.pdf BUCKET=work
# or: ./run moss -r localhost:4281 put -b sd assets/hello.txt hello.txt
```

### Download a file

```bash
make get REMOTE=hello.txt DEST=/tmp/hello.txt
make get REMOTE=photos/photo.png DEST=/tmp/photo.png
make get REMOTE=docs/assignment.pdf DEST=/tmp/assignment.pdf  BUCKET=work
# or: ./run moss -r localhost:4281 get -b sd hello.txt /tmp/hello.txt
```

### List objects in a bucket

```bash
make list
make list BUCKET=work
# or: ./run moss -r localhost:4281 list -b sd
```

### Delete an object

```bash
make rm REMOTE=hello.txt
make rm REMOTE=photos/photo.png BUCKET=work
# or: ./run moss -r localhost:4281 rm -b sd hello.txt
```

---

## Build Commands

```bash
make compile   # compile from source
make test      # run unit tests
make package   # build fat JAR (required before make up)
make clean     # remove build artefacts
make help      # list all available make targets
```

---

## Configuration Reference

| Node | Option | Description | Default |
| :--- | :--- | :--- | :--- |
| **Manager** | `-p, --port` | gRPC listening port | `4081` |
| | `-m, --mdir` | Metadata directory | `data` |
| **Volume** | `-p, --port` | gRPC listening port | `4181` |
| | `-d, --dir` | Storage directory (required) | — |
| | `-m, --manager` | Manager URL | `localhost:4081` |
| **Object** | `-p, --port` | gRPC listening port | `4281` |
| | `-d` | Database file path | `data/obj.db` |
| | `-m, --manager` | Manager URL | `localhost:4081` |

---

## Project Structure

```
src/main/proto/            gRPC service definitions
src/main/java/.../app/     Entry points for each node
src/main/java/.../core/    Domain logic and port interfaces
src/main/java/.../infra/   gRPC adapters and CLI
docker/                    Dockerfile and compose.yml
makefile                   Build, run, and client shortcuts
```
