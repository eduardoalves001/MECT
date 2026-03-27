package deti.sd.mt.ct.ui;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import deti.sd.mt.ct.core.Intersection;
import deti.sd.mt.ct.core.Vehicle;
import deti.sd.mt.ct.model.Direction;

// =============================================
//  VehicleInfoPanel - sidebar panel with vehicle status cards
// =============================================
public class VehicleInfoPanel extends JPanel {

    private final List<Vehicle> vehicles;
    private final List<Thread> vehicleThreads;

    // Pre-allocated per-vehicle UI elements
    private final JLabel[] vIdLabels;
    private final JLabel[] vPosLabels;
    private final JProgressBar[] vBatteryBars;
    private final JLabel[] vStepLabels;
    private final JLabel[] vGoalLabels;
    private final JPanel[] vPanels;

    public VehicleInfoPanel(List<Vehicle> vehicles, List<Thread> vehicleThreads) {
        this.vehicles = vehicles;
        this.vehicleThreads = vehicleThreads;

        int n = vehicles.size();
        vIdLabels    = new JLabel[n];
        vPosLabels   = new JLabel[n];
        vBatteryBars = new JProgressBar[n];
        vStepLabels  = new JLabel[n];
        vGoalLabels  = new JLabel[n];
        vPanels      = new JPanel[n];

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        setBackground(SimulationTheme.CLR_BG);

        JLabel title = new JLabel("Vehicle Status");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(6));

        // One card per vehicle
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 210)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
            card.setMaximumSize(new Dimension(260, 110));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setBackground(Color.WHITE);

            JPanel idRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            idRow.setOpaque(false);
            JPanel colorDot = new JPanel();
            colorDot.setPreferredSize(new Dimension(10, 10));
            colorDot.setBackground(SimulationTheme.vehicleColor(i));
            colorDot.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            idRow.add(colorDot);
            JLabel id = new JLabel(v.id);
            id.setFont(new Font("SansSerif", Font.BOLD, 11));
            idRow.add(id);
            vIdLabels[i] = id;
            card.add(idRow);

            JLabel pos = new JLabel("Position: \u2014");
            pos.setFont(new Font("SansSerif", Font.PLAIN, 10));
            vPosLabels[i] = pos;
            card.add(pos);

            JLabel goal = new JLabel("Goal: \u2014");
            goal.setFont(new Font("SansSerif", Font.PLAIN, 10));
            goal.setForeground(new Color(0, 100, 0));
            vGoalLabels[i] = goal;
            card.add(goal);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(100);
            bar.setStringPainted(true);
            bar.setString("Battery: 100%");
            bar.setFont(new Font("SansSerif", Font.PLAIN, 10));
            bar.setForeground(SimulationTheme.CLR_BAT_HIGH);
            bar.setMaximumSize(new Dimension(245, 18));
            vBatteryBars[i] = bar;
            card.add(Box.createVerticalStrut(3));
            card.add(bar);

            JLabel step = new JLabel("Step: 0");
            step.setFont(new Font("SansSerif", Font.PLAIN, 10));
            vStepLabels[i] = step;
            card.add(step);

            vPanels[i] = card;
            add(card);
            add(Box.createVerticalStrut(4));
        }
    }

    // ---------
    //  Refresh
    // ---------

    // Update every vehicle card with live data.
    public void refresh() {
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            boolean alive = i < vehicleThreads.size() && vehicleThreads.get(i).isAlive();

            Intersection pos = v.getCurrentPosition();
            Direction dir    = v.getCurrentDirection();
            int battery      = (int) Math.round(v.getBattery());
            int step         = v.getStep();

            // Position & direction
            vPosLabels[i].setText(pos != null
                    ? "Pos: I:" + pos.id + "  |  Dir: " + SimulationTheme.dirSymbol(dir)
                    : "N/A");

            // Goal
            Intersection goalIsec = v.getGoalIntersection();
            vGoalLabels[i].setText(goalIsec != null
                    ? "Goal: I:" + goalIsec.id
                    : "Goal: N/A");

            // Battery
            vBatteryBars[i].setValue(battery);
            vBatteryBars[i].setString("Battery: " + battery + "%");
            vBatteryBars[i].setForeground(SimulationTheme.batteryColor(battery));

            // Step
            vStepLabels[i].setText("Step: " + step);

            // Finished indicator
            if (!alive) {
                vPanels[i].setBackground(new Color(235, 235, 235));
                vIdLabels[i].setText(v.id + "  (finished)");
                vIdLabels[i].setForeground(Color.GRAY);
            }
        }
    }
}