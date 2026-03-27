package deti.sd.mt.ct.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import deti.sd.mt.ct.core.CityMap;
import deti.sd.mt.ct.core.SimulationController;
import deti.sd.mt.ct.core.Vehicle;

public class SimulationGUI extends JFrame {

    // --- Simulation references ---
    private final List<Vehicle> vehicles;
    private final List<Thread> vehicleThreads;
    private final SimulationController controller = SimulationController.getInstance();

    // --- UI components ---
    private final javax.swing.Timer refreshTimer;
    private final GridPanel gridPanel;
    private final VehicleInfoPanel vehicleInfoPanel;
    private final JLabel statsLabel;
    private final JButton startPauseButton;

    public SimulationGUI(CityMap map, List<Vehicle> vehicles, List<Thread> vehicleThreads) {
        super("MT-City Traffic Simulation");
        this.vehicles = vehicles;
        this.vehicleThreads = vehicleThreads;

        int rows = map.getRows();
        int cols = map.getCols();

        // --- Frame setup ---
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(SimulationTheme.CLR_BG);

        // Refresh timer (must be created before the WindowAdapter references it)
        refreshTimer = new javax.swing.Timer(SimulationTheme.REFRESH_MS, e -> tick());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                refreshTimer.stop();
                vehicleThreads.forEach(Thread::interrupt);
            }
        });

        startPauseButton = new JButton("Start");
        startPauseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startPauseButton.addActionListener(e -> {
            if (!controller.isStarted() || controller.isPaused()) {
                controller.startSimulation();
            } else {
                controller.pauseSimulation();
            }
            refreshStats();
        });

        // Header with control button
        add(new HeaderPanel(rows, cols, vehicles.size(), startPauseButton), BorderLayout.NORTH);

        // Grid
        gridPanel = new GridPanel(map, vehicles, vehicleThreads);
        add(gridPanel, BorderLayout.CENTER);

        // Vehicle info sidebar 
        vehicleInfoPanel = new VehicleInfoPanel(vehicles, vehicleThreads);
        JScrollPane sp = new JScrollPane(vehicleInfoPanel);
        sp.setPreferredSize(new Dimension(300, 0));
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rightPanel.add(sp, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Footer stats
        statsLabel = new JLabel("Simulation starting\u2026", SwingConstants.CENTER);
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 8, 10));
        add(statsLabel, BorderLayout.SOUTH);

        // Show
        pack();
        setResizable(true);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setVisible(true);
        refreshTimer.start();
        refreshStats();
    }

    // -----------------
    //  Periodic refresh
    // -----------------

    private void tick() {
        gridPanel.repaint();
        vehicleInfoPanel.refresh();
        refreshStats();
    }

    private void refreshStats() {
        long active = vehicleThreads.stream().filter(Thread::isAlive).count();
        long done   = vehicles.size() - active;

        if (!controller.isStarted()) {
            statsLabel.setText("Simulation not started  |  Active: " + active
                    + "  |  Finished: " + done + "  |  Total: " + vehicles.size());
            startPauseButton.setText("Start");
            return;
        }

        if (controller.isPaused()) {
            statsLabel.setText("Simulation paused  |  Active: " + active
                + "  |  Finished: " + done + "  |  Total: " + vehicles.size());
            startPauseButton.setText("Resume");
            return;
        }

        startPauseButton.setText("Pause");
        if (active > 0) {
            statsLabel.setText("Active: " + active + "  |  Finished: " + done
                    + "  |  Total: " + vehicles.size());
        } else {
            statsLabel.setText("Simulation completed \u2014 all "
                    + vehicles.size() + " vehicles finished");
            refreshTimer.stop();
        }
    }
}