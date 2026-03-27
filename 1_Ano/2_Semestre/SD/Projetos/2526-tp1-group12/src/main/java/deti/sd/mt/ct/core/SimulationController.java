package deti.sd.mt.ct.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

public class SimulationController {
    private static volatile SimulationController instance;
    private static final ReentrantLock instanceLock = new ReentrantLock();

    private volatile boolean started;
    private volatile boolean paused;
    private final ReentrantLock stateLock = new ReentrantLock();
    private final Condition stateChanged = stateLock.newCondition();

    private SimulationController(boolean startPaused) {
        this.started = !startPaused;
        this.paused = startPaused;
    }

    public static SimulationController getInstance() {
        if (instance == null) {
            instanceLock.lock();
            try {
                if (instance == null) {
                    instance = new SimulationController(false);
                }
            } finally {
                instanceLock.unlock();
            }
        }
        return instance;
    }

    public static void initialize(boolean startPaused) {
        instanceLock.lock();
        try {
            instance = new SimulationController(startPaused);
        } finally {
            instanceLock.unlock();
        }
    }

    public void startSimulation() {
        stateLock.lock();
        try {
            started = true;
            paused = false;
            stateChanged.signalAll();
        } finally {
            stateLock.unlock();
        }
    }

    public void pauseSimulation() {
        stateLock.lock();
        try {
            if (!started) {
                return;
            }
            paused = true;
        } finally {
            stateLock.unlock();
        }
    }

    public boolean isStarted() { return started; }

    public boolean isPaused() { return paused; }

    public void awaitIfPaused() throws InterruptedException {
        stateLock.lockInterruptibly();
        try {
            while (!started || paused) {
                stateChanged.await();
            }
        } finally {
            stateLock.unlock();
        }
    }

    public void sleepWithControl(long millis) throws InterruptedException {
        long remaining = millis;
        while (remaining > 0) {
            awaitIfPaused();
            long chunk = Math.min(50L, remaining);
            Thread.sleep(chunk);
            remaining -= chunk;
        }
    }

    public void awaitConditionWithPause(Lock externalLock,
                                        Condition externalCondition,
                                        BooleanSupplier shouldStillWait,
                                        long waitTimeoutMillis) throws InterruptedException {
        externalLock.unlock();
        try {
            awaitIfPaused();
        } finally {
            externalLock.lock();
        }

        if (shouldStillWait.getAsBoolean()) {
            externalCondition.await(waitTimeoutMillis, TimeUnit.MILLISECONDS);
        }
    }
}