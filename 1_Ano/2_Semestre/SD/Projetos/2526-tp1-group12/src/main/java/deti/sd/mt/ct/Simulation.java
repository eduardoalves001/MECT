package deti.sd.mt.ct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import deti.sd.mt.ct.core.ChargingStation;
import deti.sd.mt.ct.core.CityMap;
import deti.sd.mt.ct.core.Intersection;
import deti.sd.mt.ct.core.SimulationController;
import deti.sd.mt.ct.core.Vehicle;
import deti.sd.mt.ct.ui.SimulationGUI;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "mt-ct", mixinStandardHelpOptions = true, version = "25'26")
public class Simulation implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

    @Option(names = {"-t", "--time"}, description = "Time unit (ms)")
    private int timeUnit = 1;

    @Option(names = {"-r", "--rows"}, description = "City Map rows")
    private int rows = 5;

    @Option(names = {"-c", "--cols"}, description = "City Map columns")
    private int cols = 5;

    @Option(names = {"-s", "--steps"}, description = "Number of simulation steps")
    private int steps = 10;

    @Option(names = {"-v", "--vehicle"}, description = "Number of vehicles")
    private int nvehicles= 5;

    @Option(names = {"-g", "--gui"}, description = "Run with a GUI")
    private boolean showGui;

    @Override
    public void run() {
        logger.info("Starting MT-City Traffic Simulation");
        logger.info("Configuration: {}x{} grid, {} vehicles, {} steps, {} ms/time unit",
                    rows, cols, nvehicles, steps, timeUnit);

        // 1. Initialize the Grid Map
        SimulationController.initialize(showGui);
        CityMap map = new CityMap(rows, cols);

        // 2. Spawn the Charging Stations
        placeChargingStations(map);

        // 3. Launch the vehicles
        List<Thread> threads = new ArrayList<>();
        List<Vehicle> vehicleList = new ArrayList<>();

        for (int i = 0; i < nvehicles; i++) {
            // Pick a random starting intersection
            int startRow = ThreadLocalRandom.current().nextInt(rows);
            int startCol = ThreadLocalRandom.current().nextInt(cols);
            Intersection startIntersection = map.getIntersection(startRow, startCol);

            // Create vehicle
            Vehicle vehicle = new Vehicle(steps, timeUnit, startIntersection, map);
            vehicleList.add(vehicle);

            // Create thread (do not start yet)
            Thread thread = new Thread(vehicle);
            threads.add(thread);
        }

        // 4. Show GUI, if requested
        if (showGui) {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() ->
                    new SimulationGUI(map, vehicleList, threads)
                );
            } catch (Exception e) {
                logger.error("Failed to launch GUI", e);
            }
        }

        // Start vehicle threads after the window is visible
        for (Thread thread : threads) {
            thread.start();
        }

        logger.info("Launched {} vehicle threads", nvehicles);

        // 5. Wait for the vehicles threads to terminate
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Main thread interrupted while waiting for vehicle threads");
                break;
            }
        }

        logger.info("Simulation completed - all vehicles finished");
    }

    private void placeChargingStations(CityMap map) {
        int maxStations = Math.max(1, (rows * cols) / 4);
        int plugsPerStation = Math.max(1, nvehicles / 4);

        int placed = 0;
        while (placed < maxStations) {
            int r = ThreadLocalRandom.current().nextInt(rows);
            int c = ThreadLocalRandom.current().nextInt(cols);
            Intersection isec = map.getIntersection(r, c);
            if (isec.getChargingStation() != null) continue;
            map.addChargingStation(isec, new ChargingStation(plugsPerStation, timeUnit));
            placed++;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Simulation()).execute(args);
        // When the GUI is active the Swing EDT (non-daemon thread) keeps the JVM
        // alive until the window is closed. Only force-exit on errors.
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}