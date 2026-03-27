package deti.sd.mt.ct.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.mt.ct.model.Coordinate;
import deti.sd.mt.ct.model.Direction;
import deti.sd.mt.ct.model.MoveType;

public class Vehicle implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Vehicle.class);
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    private static final double LOW_BATTERY_THRESHOLD = 20.0;

    public final String id = "Vehicle-" + ID_GENERATOR.getAndIncrement();

    @Getter private final int numSteps;  // kept for GUI display only
    private final int timeUnit;
    private final CityMap map;
    private final SimulationController controller = SimulationController.getInstance();
    @Getter private volatile int step;
    @Getter private volatile double battery;
    @Getter private final int powerRequirement; // charging power load (50-100 units)

    // Current state
    @Getter private volatile Intersection currentPosition;
    @Getter private volatile Direction currentDirection;
    @Getter private volatile Intersection goalIntersection;
    private volatile Intersection chargingStationGoal;

    // Movement tracking for GUI
    @Getter private volatile Intersection traversalTarget;
    @Getter private volatile Direction traversalEntryDir;
    @Getter private volatile MoveType traversalMoveType;

    // Charging state for UI
    @Getter private volatile boolean charging = false;

    private final Deque<Direction> pendingPath = new ArrayDeque<>();

    public Vehicle(int numSteps, int timeUnit, Intersection start, CityMap map) {
        this.numSteps = numSteps;
        this.timeUnit = timeUnit;
        this.map = map;
        this.battery = 100.0;
        this.powerRequirement = generatePowerRequirement();

        this.currentPosition = start;
        Direction[] dirs = Direction.values();
        this.currentDirection = dirs[ThreadLocalRandom.current().nextInt(dirs.length)];

        this.goalIntersection = selectNewGoal();

        // DO NOT REMOVE THIS LOG
        // Must be the last instruction.
        logger.info("New vehicle with id '{}' at intersection {}", id, start.id);
    }

    @Override
    public void run() {
        // DO NOT REMOVE THIS LOG
        // Must be the first instruction.
        logger.info("Vehicle with id '{}' has started", id);
        buildPathTo(goalIntersection);

        while ((numSteps == 0 || step < numSteps) && battery > 0) {
            // DO NOT REMOVE THIS LOG
            // Must be the first instruction of the while loop.
            logger.info("SoS ({}) P:{}", step+1, battery);

            try {
                controller.awaitIfPaused();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Vehicle {} was interrupted while paused", id);
                break;
            }

            evaluateBatteryDrivenNavigation();
            ensureActivePath();

            Direction nextDir = pendingPath.poll();
            if (nextDir == null) continue;

            MoveType move = getMoveTypeToDirection(currentDirection, nextDir);
            Intersection target = computeNextIntersection(currentPosition, currentDirection, move);

            try {
                traverseIntersection(move, target);
                chargeIfAtStation();
                travelToNextIntersection();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Vehicle {} was interrupted", id);
                break;
            }

            // DO NOT REMOVE THIS LOG AND INCREMENT
            // Must be the last instructions of the while loop.
            logger.info("EoS ({}) P:{}", step+1, battery);
            step = step + 1;
        }
    }

    private void ensureActivePath() {
        if (!pendingPath.isEmpty()) return;

        if (chargingStationGoal != null) {
            if (currentPosition.id != chargingStationGoal.id) {
                buildPathTo(chargingStationGoal);
            }
        } else {
            goalIntersection = selectNewGoal();
            buildPathTo(goalIntersection);
        }
    }

    private void traverseIntersection(MoveType move, Intersection target) throws InterruptedException {
        this.traversalTarget = target;
        this.traversalEntryDir = currentDirection;
        this.traversalMoveType = move;

        currentPosition.enter(currentDirection, move, id);
        try {
            controller.sleepWithControl((long) generateTraversalTime() * timeUnit);
        } finally {
            currentPosition.exit(currentDirection, move, id);
            battery = Math.max(0.0, battery - generateTraversalConsumption());
        }

        currentPosition = target;
        currentDirection = computeNewDirection(currentDirection, move);
    }

    private void chargeIfAtStation() throws InterruptedException {
        if (chargingStationGoal == null || currentPosition.id != chargingStationGoal.id) {
            this.traversalTarget = null;
            this.charging = false;
            return;
        }
        this.charging = true;
        currentPosition.getChargingStation().useCharger(id, powerRequirement);
        battery = 100.0;
        logger.debug("Vehicle {} charged at station I:{}, battery restored to 100%", id, currentPosition.id);
        this.charging = false;
        resumeMissionAfterCharging();
    }

    private void travelToNextIntersection() throws InterruptedException {
        controller.sleepWithControl((long) generateTravelTime() * timeUnit);
        battery = Math.max(0.0, battery - generateTravelConsumption());
    }

    private Intersection selectNewGoal() {
        Intersection newGoal;

        do {
            int randomRow = ThreadLocalRandom.current().nextInt(map.getRows());
            int randomCol = ThreadLocalRandom.current().nextInt(map.getCols());
            newGoal = map.getIntersection(randomRow, randomCol);
        } while (newGoal.id == currentPosition.id);

        return newGoal;
    }

    private void buildPathTo(Intersection targetGoal) {
        if (targetGoal == null) {
            pendingPath.clear();
            return;
        }

        List<Direction> steps = map.findShortestPath(currentPosition, targetGoal);
        pendingPath.clear();
        pendingPath.addAll(steps);

        logger.debug("Vehicle {} heading to I:{} via {} steps", id, targetGoal.id, steps.size());
    }

    private void evaluateBatteryDrivenNavigation() {
        if (battery >= LOW_BATTERY_THRESHOLD || chargingStationGoal != null) {
            return;
        }

        Intersection nearestStation = map.findNearestChargerIntersection(currentPosition);
        if (nearestStation == null) {
            return;
        }
        if (nearestStation.id == currentPosition.id) {
            // already at the station - charging will be handled in the main loop on the next check
            return;
        }

        chargingStationGoal = nearestStation;
        buildPathTo(chargingStationGoal);
    }

    private void resumeMissionAfterCharging() {
        chargingStationGoal = null;
        this.traversalTarget = null;
        buildPathTo(goalIntersection);
    }

    private MoveType getMoveTypeToDirection(Direction current, Direction target) {
        if (current == target) {
            return MoveType.STRAIGHT;
        }

        if (current.opposite() == target) {
            return MoveType.U_TURN;
        }

        Direction afterLeftTurn = current.leftOf();

        return afterLeftTurn == target ? MoveType.LEFT_TURN : MoveType.RIGHT_TURN;
    }

    private Intersection computeNextIntersection(Intersection current, Direction dir, MoveType move) {
        Coordinate currentCoord = map.getCoordinate(current);
        Direction exitDir = computeNewDirection(dir, move);
        int targetRow = currentCoord.y();
        int targetCol = currentCoord.x();

        switch (exitDir) {
            case NORTH -> targetRow--;
            case SOUTH -> targetRow++;
            case EAST  -> targetCol++;
            case WEST  -> targetCol--;
        }

        return map.getIntersection(targetRow, targetCol);
    }

    private Direction computeNewDirection(Direction current, MoveType move) {
        return switch (move) {
            case STRAIGHT -> current;
            case LEFT_TURN  -> current.leftOf();
            case RIGHT_TURN -> current.rightOf();
            case U_TURN -> current.opposite();
        };
    }

    private int generateTraversalTime() {
        return ThreadLocalRandom.current().nextInt(5, 26);
    }

    private int generateTravelTime() {
        return ThreadLocalRandom.current().nextInt(10, 51);
    }

    private double generateTraversalConsumption() {
        return ThreadLocalRandom.current().nextDouble(1.0, 2.0); // 1-2% of battery
    }

    private double generateTravelConsumption() {
        return ThreadLocalRandom.current().nextDouble(2.0, 5.0); // 2-5% of battery
    }

    private static int generatePowerRequirement() {
        return ThreadLocalRandom.current().nextInt(50, 101); // 50-100 power units
    }

}