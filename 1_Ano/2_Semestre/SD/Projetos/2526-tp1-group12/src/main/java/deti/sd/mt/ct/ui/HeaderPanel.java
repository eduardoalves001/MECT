package deti.sd.mt.ct.ui;

import java.awt.*;
import javax.swing.*;

// =============================================
//  HeaderPanel - top header bar with simulation info
// =============================================
public class HeaderPanel extends JPanel {

    public HeaderPanel(int rows, int cols, int vehicleCount) {
        this(rows, cols, vehicleCount, null);
    }

    public HeaderPanel(int rows, int cols, int vehicleCount, JButton controlButton) {
        super(new BorderLayout());
        setBackground(SimulationTheme.CLR_HEADER_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JLabel title = new JLabel("MT-City Traffic Simulation");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.WEST);

        JLabel info = new JLabel(rows + " \u00d7 " + cols + " grid  |  " + vehicleCount + " vehicles");
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        info.setForeground(new Color(180, 190, 210));

        if (controlButton == null) {
            add(info, BorderLayout.EAST);
            return;
        }

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(info);
        rightPanel.add(controlButton);
        add(rightPanel, BorderLayout.EAST);
    }
}