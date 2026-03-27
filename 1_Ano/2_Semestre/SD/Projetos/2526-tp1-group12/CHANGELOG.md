# Changelog (AI GENERATED)

All notable changes to the MT-City Traffic simulation project.

---

## [Phase 0] - 2026-02-20

### Foundation and Basic Movement

**Vehicle.java**
- Implemented vehicle as thread with random initialization (position, direction)
- Added navigation system: move selection, intersection calculation, direction updates
- Implemented execution loop with timing simulation (traversal, travel times)
- Added goal-based autonomous navigation with Manhattan distance pathfinding
- Added mandatory logging (SoS, EoS, lifecycle)

**Simulation.java**
- Implemented vehicle spawning at random intersections
- Added thread creation, launching, and synchronization (`join()`)
- Added configuration logging

**Testing**
- ✅ Verified with single vehicle on 3x3 and 4x4 grids
- ✅ Goal-based navigation working (vehicles navigate purposefully towards goals)
- ✅ All mandatory logs present and correctly positioned

**Limitations (Phase 0 by design)**
- No intersection synchronization
- No conflict detection
- No battery consumption
- Tested with 1 vehicle only

---

## [Phase 1] - 2026-02-22

### Traffic Synchronization and Conflict Detection

**Intersection.java**
- Implemented thread-safe intersection entry/exit protocol using `ReentrantLock` and `Condition`
- Added conflict detection based on collision matrix (entry direction + move type)
- Implemented state tracking for vehicles currently in intersection
- Added `enter()` method with waiting on conflicts and resource acquisition
- Added `exit()` method with resource release and signaling
- Multiple vehicles can occupy same intersection simultaneously if movements don't conflict

**Vehicle.java**
- Replaced placeholder logs with actual `intersection.enter()` and `exit()` calls
- Integrated synchronization into vehicle movement cycle

**Conflict Detection**
- RIGHT_TURN: Generally safe, minimal conflicts
- STRAIGHT: Conflicts with perpendicular straight, left turns, and some U-turns
- LEFT_TURN: High conflict rate with most movements
- U_TURN: Most restrictive, conflicts with almost all movements
- Opposite direction straight movements: No conflict

**Testing**
- ✅ Verified with multiple vehicles (2-10) on various grid sizes
- ✅ Confirmed concurrent intersection occupancy for non-conflicting moves
- ✅ Verified entryCount increments/decrements correctly
- ✅ Stress tested: 10 vehicles on 3x3 grid, no deadlocks
- ✅ All mandatory logs present (>|, ><, >>, <<)

---

## [Phase 2] - 2026-02-26

### Simulation GUI

**SimulationGUI.java**
- Implemented real-time Swing GUI following the observer/polling pattern
- Custom `GridPanel` renders the city grid, roads and intersections using `Graphics2D`
- Intersection nodes are colour-coded by occupancy: empty, traversal active, high occupancy (≥ 3 vehicles)
- East sidebar displays per-vehicle state: position, direction, battery `JProgressBar`, and step counter
- GUI state refreshed every 150 ms via `javax.swing.Timer` on the EDT - vehicle threads never touch UI components

**Vehicle.java**
- Added read-only getters (`getCurrentPosition`, `getCurrentDirection`, `getBattery`, `getStep`, `getNumSteps`) for safe cross-thread state observation

**Intersection.java**
- Added `getEntryCount()` to expose current traversal count to the GUI

**Simulation.java**
- Integrated GUI launch via `SwingUtilities.invokeAndWait`, guaranteeing the window is visible before vehicle threads start
- Added `--gui` / `-g` flag; headless mode continues to work without it

**Testing**
- ✅ Tested with 2–10 vehicles on 4×4, 6×6 and 8×8 grids
- ✅ No rendering artefacts or EDT violations observed
- ✅ Vehicle movement timing unchanged in headless mode

## [Phase 3] - 2026-03-21

### Energy Management and Charging

**Vehicle.java**
- Added battery tracking, per-step consumption, and power requirement per vehicle
- Implemented logic for vehicles to re-route to nearest charging station when battery is low (bellow 20%)
- Refactored vehicle lifecycle: explicit phases for pathfinding, intersection traversal, charging, and travel
- Added robust interrupt handling and clear separation of lifecycle phases
- Improved debug logging for charging and movement

**ChargingStation.java**
- Implemented synchronized plug and power management using ReentrantLock and Condition
- Vehicles wait for available plug and sufficient power, then charge for a time proportional to energy needed
- Charging logic runs outside lock to avoid blocking other vehicles
- Added configuration for number of plugs, available power, and time unit

**Intersection.java**
- Changed enter() to throw InterruptedException, preventing negative entryCount on thread interruption
- Refactored accessors for charging station (now via Lombok @Getter)
- Improved thread safety and code clarity

**CityMap.java**
- Added support for charging station placement and nearest station search

**Simulation.java**
- Places charging stations on the map and passes time unit to ChargingStation
- Integrates all new energy management features into simulation lifecycle

**UI**
- VehicleInfoPanel and GridPanel updated to display battery state and charging station status

**Testing**
- ✅ Verified with multiple vehicles, charging stations, and various grid sizes
- ✅ No deadlocks or livelocks observed in charging or intersection logic
- ✅ All major lifecycle and energy management logs present and correct

**Refactoring and Cleanups**
- Major code cleanups for readability
- Removed redundant assignments and improved constructor clarity

### [Phase 4] - Polish and Extras

#### UI Improvements: Charging Vehicles Visualization (2026-03-22)

- Vehicles that are charging are now shown in a blue circle at the bottom right of the intersection.
- The circle displays the numbers of all vehicles currently charging at that intersection, stacked vertically.
- Charging vehicles are no longer drawn as individual icons outside the intersection.
- Traversal arrows are now suppressed for vehicles that are charging, preventing visual confusion.
- Only non-charging vehicles are drawn in the main grid; charging vehicles are represented solely in the group charging circle.

#### Simulation Pause/Resume (2026-03-25)

- Added `SimulationController` singleton to centralise pause/resume state, enforced with `ReentrantLock` (no `synchronized` keyword)
- All `Thread.sleep()` and `condition.await()` calls across `Vehicle`, `Intersection`, and `ChargingStation` replaced with pause-aware equivalents so the simulation halts correctly at every blocking point
- Added Start/Pause/Resume button to the GUI header with live status feedback in the footer
- Fixed a resource leak in `ChargingStation` where an interrupted charging thread would permanently consume a plug