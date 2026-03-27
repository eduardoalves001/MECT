# MT-City Traffic - Multithreaded City Simulation

This project is a multithreaded traffic simulation template developed for the **Sistemas Distribuídos (SD)** course at the **University of Aveiro (UAveiro)**, academic year 2025/2026.

## Overview

The simulation models a city grid where multiple autonomous vehicles navigate between intersections and charging stations. It is designed to demonstrate multithreading, synchronization, and distributed system concepts in a Java environment.

## Getting Started

### Building the Project

You can use the provided `makefile` to build and manage the project:

```bash
# Compile the source code
make compile

# Clean build artifacts
make clean
```

### Running the Simulation

Use the `run` script or the `make run` command. The simulation supports several command-line arguments thanks to [picocli](https://picocli.info/).

```bash
# Basic run with default parameters
make run

# Run with custom parameters
./run --rows 10 --cols 10 --vehicle 20 --steps 100 --time 50
```

**Note**: The `run` script compiles the project before executing.

#### Command-Line Options

| Option | Shorthand | Description | Default |
| :--- | :--- | :--- | :--- |
| `--time` | `-t` | Time unit duration in milliseconds | 1 |
| `--rows` | `-r` | Number of rows in the City Map | 5 |
| `--cols` | `-c` | Number of columns in the City Map | 5 |
| `--steps` | `-s` | Total number of simulation steps | 10 |
| `--vehicle` | `-v` | Number of vehicles to spawn | 5 |
| `--gui` | `-g` | Flag to enable the Graphical User Interface | false |

## Project Structure

- `src/main/java/deti/sd/mt/ct/Simulation.java`: Main entry point and orchestration.
- `src/main/java/deti/sd/mt/ct/core/`:
  - `CityMap.java`: Grid representation of the city.
  - `Intersection.java`: Logic for handling traffic at intersections.
  - `Vehicle.java`: Autonomous vehicle logic (Thread-based).
  - `ChargingStation.java`: Infrastructure for vehicle recharging.
- `src/main/java/deti/sd/mt/ct/model/`: Common data types like `Coordinate`, `Direction`, and `MoveType`.
- `src/main/java/deti/sd/mt/ct/ui/`: Components for the simulation GUI.

### Runtime Architecture

```
Simulation (main thread)
├── CityMap
│   └── Intersection [×rows×cols]
│       └── ChargingStation [×N]
├── Vehicle Thread [×nvehicles]
└── SimulationGUI
```

The main thread orchestrates startup: it initialises the grid, places charging stations, starts all vehicle threads, and blocks on `join()` until every vehicle finishes.

`CityMap` and the `Intersection` tree are constructed once and never structurally mutated, so no locking is needed on the map itself. Each `Intersection` and `ChargingStation` is a shared mutable resource protected by its own `ReentrantLock`, as described in the Entities and Deadlock Prevention sections.

Each `Vehicle` runs as an independent thread. Its observable state fields (`position`, `battery`, `step`, etc.) are `volatile` so the GUI can poll them safely without acquiring any lock.

`SimulationGUI` runs on the Swing EDT and only reads simulation state. It never writes to any shared resource, keeping a clean separation between the presentation layer and the concurrent core.

## Entities

The simulation uses `ReentrantLock` (fair) with `Condition` as its only synchronization mechanism.

### Simulation

- Parses CLI arguments and creates the CityMap.
- Places ChargingStations on random intersections.
- Creates one Vehicle thread per vehicle and optionally opens the GUI.
- Starts all threads, then blocks on `join()` until every vehicle finishes.

### CityMap

- Created once at startup with all intersections initialised.
- Never mutated after construction
- Exists for the full duration of the simulation.

### Intersection

- A shared resource accessed concurrently by all vehicle threads.
- Uses a `ReentrantLock` and `Condition` to control access. When a vehicle calls `enter`, it waits in a `while` loop until no conflicting movement is active, then registers itself and releases the lock. The traversal sleep happens outside the lock. On `exit`, it removes itself and calls `signalAll()`.
- Conflicts are evaluated using a collision matrix based on each vehicle's entry direction and move type (straight, left, right, U-turn).

### ChargingStation

- Attached to a randomly chosen intersection at startup. Manages a pool of plugs and a shared power budget.
- Uses a two-phase lock pattern in `useCharger`: wait and acquire resources under the lock, sleep for the charging duration outside the lock, then re-acquire to release resources and call `signalAll()`.
- Number of stations: `max(1, rows * cols / 4)`. Plugs per station: `max(1, numVehicles / 4)`. Power budget: `100 + (numPlugs - 1) * 65 +/- 15` units.

### Vehicle

- Each vehicle runs as an independent thread. Its state fields are `volatile` so the GUI can read them without locking.
- Repeats the following loop until the step limit is reached or battery hits 0:
  - Pick a goal intersection, or reroute to the nearest charger if battery is below 20%.
  - Step through the path: `enter` the next intersection, sleep for traversal time (5-25 time units, 1-2% battery), `exit`.
  - Sleep for travel time between intersections (10-50 time units, 2-5% battery).
  - If at a charging station, wait for a plug, charge to 100% (10-50 time units), then resume the original goal.
- Terminates normally when all steps are done, early if battery reaches 0, or immediately if interrupted.

## Simulation Control (Play / Pause)

The `SimulationController` singleton manages the running state of the simulation and exposes it to all vehicle threads. It is the only mechanism through which the GUI can pause and resume execution.

### State

The controller holds two `volatile` boolean fields:

- `started`: false until the user presses Start for the first time.
- `paused`: true when the simulation is paused.

When launched with `--gui`, `initialize(true)` sets `started = false` and `paused = true`, so vehicle threads block immediately and wait for the user to press Start. Without `--gui`, `initialize(false)` sets `started = true` and `paused = false`, so threads run freely from the beginning.

### How threads pause and resume

Every blocking point in every thread goes through one of two controller methods:

- `sleepWithControl(millis)`: breaks the sleep into 50 ms chunks. Before each chunk it calls `awaitIfPaused()`, which blocks on a `Condition` until the simulation is running again.
- `awaitConditionWithPause(lock, condition, check, timeout)`: used when a thread is waiting on a shared resource (intersection conflict, charging plug). It releases the external lock, calls `awaitIfPaused()`, re-acquires the lock, and only then waits on the resource condition with a short timeout.

This means every vehicle thread responds to a pause within at most 50 ms, regardless of whether it is sleeping, waiting at an intersection, or waiting at a charging station.

### GUI interaction

Pressing the button in the GUI header calls `startSimulation()` or `pauseSimulation()` on the EDT:

- `startSimulation()` sets `started = true`, `paused = false`, and calls `signalAll()` on the internal condition, waking all blocked threads.
- `pauseSimulation()` sets `paused = true`. Threads detect this on their next `awaitIfPaused()` call and block themselves.

The footer label and button text update immediately via `refreshStats()`, which reads `isStarted()` and `isPaused()` directly from the volatile fields without acquiring any lock.

## Deadlock and Starvation Prevention

### Deadlock Prevention

Deadlock requires four conditions to hold simultaneously: mutual exclusion, hold-and-wait, no preemption, and circular wait. The implementation breaks two of them:

**No hold-and-wait**: All blocking is done through `Condition.await()`, which atomically releases the held lock before the thread suspends. A vehicle never holds a lock while waiting for another resource, so the hold-and-wait condition is never satisfied.

**No circular wait**: Locks are never nested. A vehicle holds at most one lock at any given time:
- It acquires the intersection lock to enter, releases it before the traversal sleep, then re-acquires it only to exit.
- The intersection lock and the charging station lock are never held together. A vehicle always exits the intersection before calling `useCharger`.

With both conditions broken, no cycle of mutual waiting can form between threads.

### Starvation Prevention

**Fair lock ordering**: Both `Intersection` and `ChargingStation` use `ReentrantLock(true)`, which grants the lock in FIFO order. No vehicle can be indefinitely skipped when contending for a resource.

**Broadcast signalling**: `signalAll()` is used on every resource release, waking all waiting threads so each can re-evaluate its condition. Using `signal()` instead could cause a thread to wait indefinitely if the wrong waiter is selected.

**Autonomous battery management**: Vehicles reroute to the nearest charging station when battery drops below 20%, preventing them from running out of power before reaching a charger and getting stuck.