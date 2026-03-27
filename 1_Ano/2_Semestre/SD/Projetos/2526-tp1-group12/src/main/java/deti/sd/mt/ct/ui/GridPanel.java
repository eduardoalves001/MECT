package deti.sd.mt.ct.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import deti.sd.mt.ct.core.ChargingStation;
import deti.sd.mt.ct.core.CityMap;
import deti.sd.mt.ct.core.Intersection;
import deti.sd.mt.ct.core.Vehicle;
import deti.sd.mt.ct.model.Coordinate;
import deti.sd.mt.ct.model.Direction;
import deti.sd.mt.ct.model.MoveType;

// =============================================
//  GridPanel - custom painting of the city grid
// =============================================
public class GridPanel extends JPanel {

    private static final int   MIN_CELL  = 90;
    private static final int   MAX_CELL  = 140;
    private static final int   PAD       = 50;
    /** Master scale factor: reduce to shrink vehicles and arrows, increase to enlarge them. */
    private static final float GUI_SCALE = 0.60f;

    private final CityMap map;
    private final List<Vehicle> vehicles;
    private final List<Thread> vehicleThreads;
    private final int rows, cols;

    // Preallocated snapshot arrays (resized if needed)
    private int snapSize;
    private Intersection[] vPos, vGoal, vTravTgt;
    private Direction[]    vDir, vTravDir;
    private MoveType[]     vTravMov;
    private boolean[]      vAlive;
    private boolean[]      vCharging;
    private final Map<Integer, List<Integer>> waitingAt    = new HashMap<>();
    private final Map<Integer, List<Integer>> traversingAt = new HashMap<>();
    private final Map<Integer, List<Integer>> goalsAt      = new HashMap<>();

    public GridPanel(CityMap map, List<Vehicle> vehicles, List<Thread> vehicleThreads) {
        this.map = map;
        this.vehicles = vehicles;
        this.vehicleThreads = vehicleThreads;
        this.rows = map.getRows();
        this.cols = map.getCols();
        int cell = cellSize();
        setPreferredSize(new Dimension(
                Math.max(cols * cell + 2 * PAD, 560),
                Math.max(rows * cell + 2 * PAD, 440)));
        setBackground(SimulationTheme.CLR_BG);
    }

    // Used once at construction time to compute the initial preferred size for pack().
    private int cellSize() {
        return Math.max(MIN_CELL, Math.min(MAX_CELL, 700 / Math.max(rows, cols)));
    }

    // No min/max caps — fills available space at any window size.
    private int cellSize(int panelW, int panelH) {
        int cw = (panelW - 2 * PAD) / cols;
        int ch = (panelH - 2 * PAD) / rows;
        return Math.max(20, Math.min(cw, ch));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int cell = cellSize(getWidth(), getHeight());
        int ox   = (getWidth()  - cols * cell) / 2;
        int oy   = (getHeight() - rows * cell) / 2;
        int rad  = cell * 7 / 20;
        int numV = vehicles.size();

        snapshotVehicles(numV);
        classifyVehicles(numV);
        drawRoads(g2, cell, ox, oy);
        drawAllIntersections(g2, cell, rad, ox, oy);
        drawAllVehicles(g2, cell, rad, ox, oy, numV);
    }

    private void snapshotVehicles(int numV) {
        if (numV > snapSize) {
            snapSize = numV;
            vPos     = new Intersection[numV];
            vDir     = new Direction[numV];
            vGoal    = new Intersection[numV];
            vTravTgt = new Intersection[numV];
            vTravDir = new Direction[numV];
            vTravMov = new MoveType[numV];
            vAlive   = new boolean[numV];
            vCharging = new boolean[numV];
        }
        for (int i = 0; i < numV; i++) {
            Vehicle v   = vehicles.get(i);
            vAlive[i]   = i >= vehicleThreads.size() || vehicleThreads.get(i).isAlive();
            vPos[i]     = v.getCurrentPosition();
            vDir[i]     = v.getCurrentDirection();
            vGoal[i]    = v.getGoalIntersection();
            vTravTgt[i] = v.getTraversalTarget();
            vTravDir[i] = v.getTraversalEntryDir();
            vTravMov[i] = v.getTraversalMoveType();
            vCharging[i] = false;
            try {
                vCharging[i] = v.isCharging();
            } catch (Exception ignored) {}
        }
    }

    private void classifyVehicles(int numV) {
        waitingAt.clear();
        traversingAt.clear();
        goalsAt.clear();
        for (int i = 0; i < numV; i++) {
            if (!vAlive[i]) continue;
            if (vTravTgt[i] != null && vPos[i] != null)
                traversingAt.computeIfAbsent(vPos[i].id, k -> new ArrayList<>()).add(i);
            else if (vPos[i] != null)
                waitingAt.computeIfAbsent(vPos[i].id, k -> new ArrayList<>()).add(i);
            if (vGoal[i] != null)
                goalsAt.computeIfAbsent(vGoal[i].id, k -> new ArrayList<>()).add(i);
        }
    }

    private void drawRoads(Graphics2D g2, int cell, int ox, int oy) {
        g2.setColor(SimulationTheme.CLR_ROAD);
        g2.setStroke(new BasicStroke(Math.max(2, cell / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int cx = ox + c * cell + cell / 2, cy = oy + r * cell + cell / 2;
                if (c < cols - 1) g2.drawLine(cx, cy, cx + cell, cy);
                if (r < rows - 1) g2.drawLine(cx, cy, cx, cy + cell);
            }
    }

    private void drawAllIntersections(Graphics2D g2, int cell, int rad, int ox, int oy) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Intersection isec = map.getIntersection(r, c);
                if (isec == null) continue;
                int cx = ox + c * cell + cell / 2, cy = oy + r * cell + cell / 2;
                drawIntersection(g2, cx, cy, rad, isec.id, isec.getChargingStation(),
                        traversingAt.getOrDefault(isec.id, Collections.emptyList()),
                        goalsAt.getOrDefault(isec.id, Collections.emptyList()),
                        vTravDir, vTravMov);
            }
    }

    private void drawAllVehicles(Graphics2D g2, int cell, int rad, int ox, int oy, int numV) {
        for (int i = 0; i < numV; i++) {
            if (!vAlive[i] || vPos[i] == null) continue;
            if (vCharging[i]) continue; // Charging vehicles are now shown in the intersection circle only
            Coordinate pc = map.getCoordinate(vPos[i]);
            int icx = ox + pc.x() * cell + cell / 2;
            int icy = oy + pc.y() * cell + cell / 2;

            if (vTravTgt[i] != null) {
                drawTraversingVehicle(g2, i, icx, icy, rad);
            } else {
                drawIdleVehicle(g2, i, icx, icy, rad);
            }
        }
    }

    // (Removed per-vehicle charging icon; now only group circle is shown)

    // Vehicle is traversing currentPosition — show it displaced toward the exit road.
    private void drawTraversingVehicle(Graphics2D g2, int i, int icx, int icy, int rad) {
        Direction entryD = vTravDir[i] != null ? vTravDir[i] : vDir[i];
        MoveType  movT   = vTravMov[i] != null ? vTravMov[i] : MoveType.STRAIGHT;
        Direction exitD  = exitDirection(entryD, movT);
        List<Integer> travList = traversingAt.getOrDefault(vPos[i].id, Collections.emptyList());
        int tIdx    = Math.max(0, travList.indexOf(i));
        int tCount  = Math.max(1, travList.size());
        int perpOff = tCount > 1 ? (int) Math.round((tIdx - (tCount - 1) / 2.0) * (rad / 4.0)) : 0;
        int vx = icx + dirDx(exitD) * (rad / 2) + dirDx(perpDir(exitD)) * perpOff;
        int vy = icy + dirDy(exitD) * (rad / 2) + dirDy(perpDir(exitD)) * perpOff;
        drawVehicle(g2, vx, vy, i, exitD, rad);
    }

    // Vehicle is waiting/travelling — show it on the approach road it arrived from.
    private void drawIdleVehicle(Graphics2D g2, int i, int icx, int icy, int rad) {
        Direction d = vDir[i] != null ? vDir[i] : Direction.EAST;
        List<Integer> others = waitingAt.getOrDefault(vPos[i].id, Collections.emptyList());
        int sameIdx = 0;
        for (int j : others) {
            if ((vDir[j] != null ? vDir[j] : Direction.EAST) == d) {
                if (j == i) break;
                sameIdx++;
            }
        }
        Direction approachDir = d.opposite();
        Coordinate vc = map.getCoordinate(vPos[i]);
        boolean onRoad = map.getIntersection(vc.y() + dirDy(approachDir), vc.x() + dirDx(approachDir)) != null;
        int vx, vy;
        if (onRoad) {
            int dist = rad + rad / 3 + sameIdx * (rad / 3);
            vx = icx + dirDx(approachDir) * dist;
            vy = icy + dirDy(approachDir) * dist;
        } else {
            // At grid boundary: keep inside the circle
            vx = icx + dirDx(perpDir(d)) * (sameIdx * rad / 4);
            vy = icy + dirDy(perpDir(d)) * (sameIdx * rad / 4);
        }
        drawVehicle(g2, vx, vy, i, d, rad);
    }

    //---------------------------------------------------------
    //  Intersection: label, center=goal diamonds, traversals
    //---------------------------------------------------------
    private void drawIntersection(Graphics2D g2, int cx, int cy, int rad,
                                  int isecId, ChargingStation station,
                                  List<Integer> travList, List<Integer> goalList,
                                  Direction[] travDir, MoveType[] travMov) {
        g2.setColor(station != null ? SimulationTheme.CLR_CHARGER : Color.WHITE);
        g2.fillOval(cx - rad, cy - rad, 2 * rad, 2 * rad);
        g2.setColor(new Color(90, 100, 120));
        g2.setStroke(new BasicStroke(Math.max(1f, rad / 20f)));
        g2.drawOval(cx - rad, cy - rad, 2 * rad, 2 * rad);

        for (int idx = 0; idx < travList.size(); idx++) {
            int vi = travList.get(idx);
            // Do not draw traversal arrow if vehicle is charging at this intersection
            if (vCharging[vi] && vPos[vi] != null && vPos[vi].id == isecId) continue;
            drawTraversalArrow(g2, cx, cy, rad, travDir[vi], travMov[vi], vi, idx, travList.size());
        }

        // Draw charging vehicles circle at bottom right diagonal if any
        List<Integer> chargingHere = new ArrayList<>();
        for (int i = 0; i < vehicles.size(); i++) {
            if (vPos[i] != null && vPos[i].id == isecId && vCharging[i]) {
                chargingHere.add(i);
            }
        }
        if (!chargingHere.isEmpty()) {
            int circleR = Math.max(14, rad / 2);
            int circleCx = cx + rad - circleR / 2;
            int circleCy = cy + rad - circleR / 2;
            // Light blue-green color
            g2.setColor(new Color(180, 240, 255, 220));
            g2.fillOval(circleCx - circleR, circleCy - circleR, 2 * circleR, 2 * circleR);
            g2.setColor(new Color(60, 180, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(circleCx - circleR, circleCy - circleR, 2 * circleR, 2 * circleR);
            // Draw vehicle numbers inside, spaced vertically
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, circleR / 2)));
            FontMetrics fm = g2.getFontMetrics();
            int total = chargingHere.size();
            int startY = circleCy - (total * fm.getAscent()) / 2 + fm.getAscent();
            for (int idx = 0; idx < total; idx++) {
                String id = String.valueOf(chargingHere.get(idx));
                int y = startY + idx * fm.getAscent();
                g2.drawString(id, circleCx - fm.stringWidth(id) / 2, y);
            }
        }

        // Intersection label
        int labelFont = Math.max(7, rad / 4);
        g2.setFont(new Font("SansSerif", Font.PLAIN, labelFont));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(100, 100, 120));
        g2.drawString("I" + isecId, cx - rad, cy + rad + fm.getAscent() + 1);

        // Charging station plug label
        if (station != null) {
            int inUse = station.getNumPlugs() - station.getAvailablePlugs();
            String plugLabel = inUse + "/" + station.getNumPlugs();
            g2.setColor(new Color(120, 85, 0));
            g2.drawString(plugLabel, cx - rad, cy + rad + fm.getAscent() * 2 + 2);
        }

        // Goal diamonds
        if (!goalList.isEmpty()) {
            int ds = Math.max(4, rad / 6);
            int spacing = ds * 2 + 2;
            int maxPerRow = Math.max(1, (2 * rad - 4) / spacing);
            int numGoals = goalList.size();
            int numRows = (numGoals + maxPerRow - 1) / maxPerRow;
            int startY = cy - (numRows * spacing) / 2 + ds + 2;
            for (int i = 0; i < numGoals; i++) {
                int row = i / maxPerRow;
                int col = i % maxPerRow;
                int rowCount = Math.min(maxPerRow, numGoals - row * maxPerRow);
                int dx = cx - (rowCount * spacing) / 2 + col * spacing + spacing / 2;
                int dy = startY + row * spacing;
                drawGoalDiamond(g2, dx, dy, goalList.get(i), ds);
            }
        }
    }

    //---------------------------------------
    //  Goal diamond (small, with vehicle ID)
    //---------------------------------------
    private void drawGoalDiamond(Graphics2D g2, int cx, int cy, int vi, int s) {
        Color c = SimulationTheme.vehicleColor(vi);
        int[] xs = {cx, cx + s, cx, cx - s};
        int[] ys = {cy - s, cy, cy + s, cy};
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
        g2.fillPolygon(xs, ys, 4);
        g2.setColor(c.darker());
        g2.setStroke(new BasicStroke(Math.max(1f, s / 5f)));
        g2.drawPolygon(xs, ys, 4);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(6, s)));
        FontMetrics fm = g2.getFontMetrics();
        String id = String.valueOf(vi);
        g2.drawString(id, cx - fm.stringWidth(id) / 2, cy + fm.getAscent() / 2 - 1);
    }

    //-------------------------------------------------------
    //  Vehicle icon (colored rect + direction triangle + ID)
    //-------------------------------------------------------
    private void drawVehicle(Graphics2D g2, int cx, int cy, int vi, Direction d, int rad) {
        Color c = SimulationTheme.vehicleColor(vi);
        int w = Math.max(8, (int)(rad * 2 / 3f * GUI_SCALE));
        int h = Math.max(5, (int)(rad * 2 / 5f * GUI_SCALE));
        if (d == Direction.NORTH || d == Direction.SOUTH) { int t = w; w = h; h = t; }

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRoundRect(cx - w / 2 + 1, cy - h / 2 + 1, w, h, 4, 4);
        g2.setColor(c);
        g2.fillRoundRect(cx - w / 2, cy - h / 2, w, h, 4, 4);
        g2.setColor(c.darker());
        g2.setStroke(new BasicStroke(Math.max(0.8f, rad / 30f)));
        g2.drawRoundRect(cx - w / 2, cy - h / 2, w, h, 4, 4);

        // Direction triangle
        g2.setColor(Color.WHITE);
        int s = Math.max(2, h / 4);
        switch (d) {
            case NORTH -> g2.fillPolygon(new int[]{cx, cx - s, cx + s}, new int[]{cy - h/2 + 1, cy - h/2 + s + 1, cy - h/2 + s + 1}, 3);
            case SOUTH -> g2.fillPolygon(new int[]{cx, cx - s, cx + s}, new int[]{cy + h/2 - 1, cy + h/2 - s - 1, cy + h/2 - s - 1}, 3);
            case EAST  -> g2.fillPolygon(new int[]{cx + w/2 - 1, cx + w/2 - s - 1, cx + w/2 - s - 1}, new int[]{cy, cy - s, cy + s}, 3);
            case WEST  -> g2.fillPolygon(new int[]{cx - w/2 + 1, cx - w/2 + s + 1, cx - w/2 + s + 1}, new int[]{cy, cy - s, cy + s}, 3);
        }

        // Vehicle ID
        int fontSize = Math.max(6, rad / 5);
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String id = String.valueOf(vi);
        g2.drawString(id, cx - fm.stringWidth(id) / 2, cy + fm.getAscent() / 2 - 1);
    }

    private void drawTraversalArrow(Graphics2D g2, int cx, int cy, int rad,
                                    Direction entryDir, MoveType move, int vi,
                                    int idx, int total) {
        if (entryDir == null || move == null) return;
        Direction exitDir   = exitDirection(entryDir, move);
        Direction entrySide = entryDir.opposite();

        int offsetPx = total > 1 ? (int) Math.round((idx - (total - 1) / 2.0) * (rad / 10.0)) : 0;
        int margin   = rad * 9 / 10;
        // perpendicular offset for multi-vehicle separation (shared axis for all move types)
        int offX = dirDx(perpDir(entryDir)) * offsetPx;
        int offY = dirDy(perpDir(entryDir)) * offsetPx;

        Color vc = SimulationTheme.vehicleColor(vi);
        g2.setColor(new Color(vc.getRed(), vc.getGreen(), vc.getBlue(), 210));

        float strokeThin   = Math.max(0.8f, rad / 20f * GUI_SCALE);
        float strokeNormal = Math.max(0.8f, rad / 15f * GUI_SCALE);
        float strokeThick  = Math.max(0.8f, rad / 12f * GUI_SCALE);
        int   tipLen       = Math.max(3, (int)(rad / 6f * GUI_SCALE));

        // -- U_TURN: hairpin — unchanged --------------------------------------
        if (move == MoveType.U_TURN) {
            g2.setStroke(new BasicStroke(strokeThick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Direction perp = perpDir(entrySide);
            int gap   = Math.max(4, rad / 3);
            int depth = (int)(margin * 0.9);

            int ex1x = cx + dirDx(entrySide)*margin - dirDx(perp)*gap + dirDx(perp)*offsetPx;
            int ex1y = cy + dirDy(entrySide)*margin - dirDy(perp)*gap + dirDy(perp)*offsetPx;
            int ex2x = cx + dirDx(entrySide)*margin + dirDx(perp)*gap + dirDx(perp)*offsetPx;
            int ex2y = cy + dirDy(entrySide)*margin + dirDy(perp)*gap + dirDy(perp)*offsetPx;
            int  c1x = cx - dirDx(entrySide)*depth - dirDx(perp)*gap + dirDx(perp)*offsetPx;
            int  c1y = cy - dirDy(entrySide)*depth - dirDy(perp)*gap + dirDy(perp)*offsetPx;
            int  c2x = cx - dirDx(entrySide)*depth + dirDx(perp)*gap + dirDx(perp)*offsetPx;
            int  c2y = cy - dirDy(entrySide)*depth + dirDy(perp)*gap + dirDy(perp)*offsetPx;

            g2.draw(new CubicCurve2D.Double(ex1x, ex1y, c1x, c1y, c2x, c2y, ex2x, ex2y));
            drawArrowTip(g2, ex2x, ex2y, Math.atan2(dirDy(entrySide), dirDx(entrySide)), tipLen);
            return;
        }

        // --- STRAIGHT: right-lane offset (drive on the right side) ------------
        if (move == MoveType.STRAIGHT) {
            int laneOff = rad / 5;
            Direction right = entryDir.rightOf();
            int lx = dirDx(right) * laneOff + offX;
            int ly = dirDy(right) * laneOff + offY;
            int x1 = cx + dirDx(entrySide) * margin + lx;
            int y1 = cy + dirDy(entrySide) * margin + ly;
            int x2 = cx + dirDx(entryDir)  * margin + lx;
            int y2 = cy + dirDy(entryDir)  * margin + ly;
            g2.setStroke(new BasicStroke(strokeNormal, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
            drawArrowTip(g2, x2, y2, Math.atan2(dirDy(entryDir), dirDx(entryDir)), tipLen);
            return;
        }

        // --- RIGHT_TURN / LEFT_TURN: straight to center, then straight to exit --
        int x1      = cx + dirDx(entrySide) * margin + offX;
        int y1      = cy + dirDy(entrySide) * margin + offY;
        int cornerX = cx + offX;
        int cornerY = cy + offY;
        int x2      = cx + dirDx(exitDir)   * margin + offX;
        int y2      = cy + dirDy(exitDir)   * margin + offY;
        float stroke = (move == MoveType.RIGHT_TURN) ? strokeThin : strokeThick;
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(cornerX, cornerY);
        path.lineTo(x2, y2);
        g2.draw(path);
        drawArrowTip(g2, x2, y2, Math.atan2(dirDy(exitDir), dirDx(exitDir)), tipLen);
    }

    private void drawArrowTip(Graphics2D g2, int x, int y, double angle, int len) {
        double sp = Math.PI / 5;
        g2.fillPolygon(
                new int[]{x, x - (int)(len * Math.cos(angle - sp)), x - (int)(len * Math.cos(angle + sp))},
                new int[]{y, y - (int)(len * Math.sin(angle - sp)), y - (int)(len * Math.sin(angle + sp))}, 3);
    }

    //---------------------
    //  Direction utilities
    //---------------------
    private static Direction exitDirection(Direction entry, MoveType move) {
        return switch (move) {
            case STRAIGHT   -> entry;
            case LEFT_TURN  -> entry.leftOf();
            case RIGHT_TURN -> entry.rightOf();
            case U_TURN     -> entry.opposite();
        };
    }

    private static Direction perpDir(Direction d) {
        return switch (d) { case NORTH, SOUTH -> Direction.EAST; case EAST, WEST -> Direction.NORTH; };
    }

    private static int dirDx(Direction d) {
        return switch (d) { case EAST -> 1; case WEST -> -1; default -> 0; };
    }

    private static int dirDy(Direction d) {
        return switch (d) { case SOUTH -> 1; case NORTH -> -1; default -> 0; };
    }
}