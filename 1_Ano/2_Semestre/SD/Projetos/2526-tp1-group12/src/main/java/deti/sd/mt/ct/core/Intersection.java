package deti.sd.mt.ct.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.mt.ct.model.Direction;
import deti.sd.mt.ct.model.MoveType;

public class Intersection {
    private static final Logger logger = LoggerFactory.getLogger(Intersection.class);
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    public final int id = ID_GENERATOR.getAndIncrement();

    private final AtomicInteger entryCount = new AtomicInteger(0);
    private ChargingStation station = null;

    private final Lock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();
    private final List<VehicleMovement> currentVehicles = new ArrayList<>();
    private final SimulationController controller = SimulationController.getInstance();

    private record VehicleMovement(String vehicleId, Direction entry, MoveType move) {}

    /// A vehicle must call this function to enter the intersection.
    ///
    /// YOU MUST USE THIS FUNCTION
    public void enter(Direction entry, MoveType move, String vehicleId) throws InterruptedException {
        // DO NOT REMOVE THIS LOG
        // Must be the first instruction.
        logger.info(">| I:{} (?) V:{} E:{}, M:{}", this.id, vehicleId, entry, move);

        lock.lock();
        // --- critical section begin ---
        try {
            // Block until no conflicting vehicle is inside the intersection
            while (hasConflict(entry, move)) {
                logger.debug("~~ I:{} V:{} waiting (conflict E:{}, M:{})", this.id, vehicleId, entry, move);
                controller.awaitConditionWithPause(lock, condition, () -> hasConflict(entry, move), 50L);
            }

            int n = entryCount.incrementAndGet();
            logger.info(">< I:{} ({}) V:{} E:{}, M:{}", this.id, n, vehicleId, entry, move);
            currentVehicles.add(new VehicleMovement(vehicleId, entry, move));

            // DO NOT REMOVE THIS LOG
            // Must be the last instruction of the enter function.
            // It signals that the vehicle has the resources to traverse the intersection.
            logger.info(">> I:{} ({}) V:{} E:{}, M:{}", this.id, n, vehicleId, entry, move);
        // --- critical section end ---
        } finally {
            lock.unlock();
        }
    }

    /// A vehicle must call this function to exit the intersection.
    ///
    /// YOU MUST USE THIS FUNCTION
    public void exit(Direction entry, MoveType move, String vehicleId) {
        lock.lock();
        // --- critical section begin ---
        try {
            currentVehicles.removeIf(v -> v.vehicleId.equals(vehicleId));
            condition.signalAll();

            // DO NOT REMOVE THIS LOG AND DECREMENT
            // Must be the last instructions of the exit function.
            int n = entryCount.decrementAndGet();
            logger.info("<< I:{} ({}) V:{} E:{}, M:{}", this.id, n, vehicleId, entry, move);
        // --- critical section end ---
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the requested movement conflicts with any current vehicles in the intersection.
     */
    private boolean hasConflict(Direction entry, MoveType move) {
        for (VehicleMovement v : currentVehicles) {
            if (conflicts(entry, move, v.entry, v.move)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if two movements conflict based on the collision matrix.
     * Implements the collision matrix using relative direction roles
     * (oncoming / right-side / left-side) so the same rules apply symmetrically
     * regardless of the physical N/S/E/W entry.
     *
     * For a vehicle entering from direction d1:
     *   oncoming  = d1.opposite()
     *   rightSide = 90° clockwise of d1   (N→E, E→S, S→W, W→N)
     *   leftSide  = 90° counter-clockwise (N→W, W→S, S→E, E→N)
     *
     * Conflict sets (move2 values that collide with each move1, by relative role):
     *
     *          vs oncoming   vs rightSide   vs leftSide
     * STRAIGHT   L, U         S, L, U        all
     * LEFT_TURN  all          S, L, U        all
     * RIGHT_TURN L, U         S, U           U
     * U_TURN     all          all            all
     */
    private boolean conflicts(Direction entry1, MoveType move1, Direction entry2, MoveType move2) {
        if (entry1 == entry2) return false;

        Direction rightSide = entry1.rightOf();

        EnumSet<MoveType> conflicting;

        if (entry2 == entry1.opposite()) {
            // Oncoming traffic
            conflicting = switch (move1) {
                case STRAIGHT  -> EnumSet.of(MoveType.LEFT_TURN, MoveType.U_TURN);
                case LEFT_TURN -> EnumSet.allOf(MoveType.class);
                case RIGHT_TURN -> EnumSet.of(MoveType.LEFT_TURN, MoveType.U_TURN);
                case U_TURN    -> EnumSet.allOf(MoveType.class);
            };
        } else if (entry2 == rightSide) {
            // Traffic from the right side
            conflicting = switch (move1) {
                case STRAIGHT  -> EnumSet.of(MoveType.STRAIGHT, MoveType.LEFT_TURN, MoveType.U_TURN);
                case LEFT_TURN -> EnumSet.of(MoveType.STRAIGHT, MoveType.LEFT_TURN, MoveType.U_TURN);
                case RIGHT_TURN -> EnumSet.of(MoveType.STRAIGHT, MoveType.U_TURN);
                case U_TURN    -> EnumSet.allOf(MoveType.class);
            };
        } else {
            // Traffic from the left side (entry2 == leftSide)
            conflicting = switch (move1) {
                case STRAIGHT  -> EnumSet.allOf(MoveType.class);
                case LEFT_TURN -> EnumSet.allOf(MoveType.class);
                case RIGHT_TURN -> EnumSet.of(MoveType.U_TURN);
                case U_TURN    -> EnumSet.allOf(MoveType.class);
            };
        }

        return conflicting.contains(move2);
    }

    /// Sets the charging stations for this intersection.
    public void setChargingStation(ChargingStation station) {
        this.station = station;
        logger.info("Intersection {} now has charging station {}", id, station.id);
    }

    /// Returns the charging stations of this intersection.
    public ChargingStation getChargingStation() {
        return station;
    }

    /// Check if this intersection has a charging station.
    public boolean hasChargingStation() {
        return station != null;
    }

    /// Returns the current number of vehicles inside the intersection.
    public int getEntryCount() {
        return entryCount.get();
    }
}
