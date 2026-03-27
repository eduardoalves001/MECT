package deti.sd.mt.ct.core;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChargingStation {
    private static final Logger logger = LoggerFactory.getLogger(ChargingStation.class);
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    public final int id = ID_GENERATOR.getAndIncrement();

    @Getter private final int numPlugs;
    private final int timeUnit;

    @Getter private int availablePlugs;
    private int availablePower;

    private final Lock lock = new ReentrantLock(true);
    private final Condition resourceAvailable = lock.newCondition();
    private final SimulationController controller = SimulationController.getInstance();

    public ChargingStation(int numPlugs, int timeUnit) {
        this.numPlugs = numPlugs;
        this.timeUnit = timeUnit;
        this.availablePlugs = numPlugs;
        int basePower = 100 + (numPlugs - 1) * 65;
        this.availablePower = basePower + ThreadLocalRandom.current().nextInt(-15, 16);

        // DO NOT REMOVE THIS LOG
        // Must be the last instruction
        logger.info("New charging station with id:{} plugs:{} power:{}",
                this.id, numPlugs, availablePower);
    }

    public void useCharger(String vehicleId, int amountNeeded) throws InterruptedException {
        lock.lock();
        try {
            // DO NOT REMOVE THIS LOG
            // Must be the first instruction after enter the critical region
            logger.info(">| C:{} V:{} S:{} P:{} A:{}", this.id, vehicleId, availablePlugs, amountNeeded, this.availablePower);

            // 1. Wait until a plug is free AND the station has enough power
            while (availablePlugs == 0 || availablePower < amountNeeded) {
                // DO NOT REMOVE THIS LOG
                // NOTE: Must be shown when waiting for the resources.
                logger.info(">< C:{} V:{} S:{} P:{} A:{}", this.id, vehicleId, availablePlugs, amountNeeded, this.availablePower);
                controller.awaitConditionWithPause(
                        lock,
                        resourceAvailable,
                        () -> availablePlugs == 0 || availablePower < amountNeeded,
                        50L);
            }

            // 2. Acquire resources
            availablePlugs--;
            availablePower -= amountNeeded;

            // DO NOT REMOVE THIS LOG
            // NOTE: Must be shown after acquiring the resources
            logger.info(">> C:{} V:{} S:{} P:{} A:{}", this.id, vehicleId, availablePlugs, amountNeeded, this.availablePower);
        } finally {
            lock.unlock();
        }

        // 3. Simulate charging (outside the lock to avoid blocking other vehicles)
        int chargeTime = ThreadLocalRandom.current().nextInt(10, 51);
        try {
            controller.sleepWithControl((long) chargeTime * timeUnit);
        } finally {
            // 4. Release resources and notify waiting vehicles
            lock.lock();
            try {
                availablePlugs++;
                availablePower += amountNeeded;
                resourceAvailable.signalAll();

                // DO NOT REMOVE THIS LOG
                // NOTE: Must be shown after releasing the resources
                logger.info("<< C:{} V:{} S:{} P:{} A:{}", this.id, vehicleId, availablePlugs, amountNeeded, this.availablePower);
            } finally {
                lock.unlock();
            }
        }
    }

    /// Returns the current available power
    public double getPowerLevel() {
        return availablePower;
    }
}
