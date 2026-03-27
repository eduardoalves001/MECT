package deti.sd.mt.ct.core;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import deti.sd.mt.ct.model.Coordinate;
import deti.sd.mt.ct.model.Direction;

public class CityMap {
    private static final Logger logger = LoggerFactory.getLogger(CityMap.class);

    @Getter private final int rows;
    @Getter private final int cols;

    private final List<Intersection> grid = new ArrayList<>();

    public CityMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        for (int i = 0; i < rows * cols; i++) {
            grid.add(new Intersection());
        }

        // DO NOT REMOVE THIS LOG
        // Must be the last instruction.
        logger.info("New CityMap with {} rows and {} columns", rows, cols);
    }

    /// Add the `station` to the intersection `isec`
    public void addChargingStation(Intersection isec, ChargingStation station) {
        isec.setChargingStation(station);
    }

    /// Returns the intersection located at row `r` and column `c`.
    public Intersection getIntersection(int r, int c) {
        if (r >= 0 && c >= 0 && r < rows && c < cols)
            return grid.get(c + cols*r);
        return null;
    }

    /// Returns the intersection object by `id`.
    public Intersection getIntersection(int id) {
        if (id >= 0 &&  id < rows*cols)
            return grid.get(id);
        return null;
    }

    /// Returns the intersection object located at coordinate `coord`.
    public Intersection getIntersection(Coordinate coord) {
        int idx = coord.x() + coord.y() * cols;
        if (idx >= 0 &&  idx < rows*cols)
            return grid.get(idx);
        return null;
    }

    /// Returns the coordinates of an intersection.
    public Coordinate getCoordinate(Intersection isec) {
        return new Coordinate(isec.id % cols, isec.id / cols);
    }

    /// Returns the nearest intersection to the `current` intersection.
    public Intersection findNearestChargerIntersection(Intersection current) {
        Coordinate from = getCoordinate(current);
        Intersection nearest = null;
        int bestDist = Integer.MAX_VALUE;

        for (Intersection isec : grid) {
            if (isec.getChargingStation() == null) continue;
            Coordinate c = getCoordinate(isec);
            int dist = Math.abs(c.x() - from.x()) + Math.abs(c.y() - from.y());
            if (dist < bestDist) {
                bestDist = dist;
                nearest = isec;
            }
        }
        return nearest;
    }

    public List<Direction> findShortestPath(Intersection current, Intersection goal) {
        Coordinate from = this.getCoordinate(current);
        Coordinate to   = this.getCoordinate(goal);
        int dc = to.x() - from.x();
        int dr = to.y() - from.y();

        List<Direction> steps = new ArrayList<>();
        Direction h = dc > 0 ? Direction.EAST  : Direction.WEST;
        Direction v = dr > 0 ? Direction.SOUTH : Direction.NORTH;
        for (int i = 0; i < Math.abs(dc); i++) steps.add(h);
        for (int i = 0; i < Math.abs(dr); i++) steps.add(v);

        Collections.shuffle(steps, ThreadLocalRandom.current());
        return steps;
    }
}