package deti.sd.mt.ct.ui;

import java.awt.Color;

import deti.sd.mt.ct.model.Direction;

// =============================================
//  SimulationTheme - shared UI color palette and utility methods
// =============================================
public final class SimulationTheme {

    // Background
    public static final Color CLR_BG            = new Color(245, 245, 250);

    // Header
    public static final Color CLR_HEADER_BG     = new Color(50, 60, 80);

    // Roads
    public static final Color CLR_ROAD          = new Color(180, 180, 190);

    // Charging station fill
    public static final Color CLR_CHARGER       = new Color(255, 230, 160);

    // Battery levels
    public static final Color CLR_BAT_HIGH      = new Color(50, 160, 50);
    public static final Color CLR_BAT_MED       = new Color(200, 160, 0);
    public static final Color CLR_BAT_LOW       = new Color(200, 50, 50);

    // Refresh interval (ms)
    public static final int REFRESH_MS = 150;

    // ----------------
    //  Utility helpers
    // ----------------

    private SimulationTheme() {  }

    public static String dirSymbol(Direction d) {
        if (d == null) return "?";
        return switch (d) {
            case NORTH -> "\u2191 N";
            case SOUTH -> "\u2193 S";
            case EAST  -> "\u2192 E";
            case WEST  -> "\u2190 W";
        };
    }

    public static Color batteryColor(int battery) {
        if (battery > 60) return CLR_BAT_HIGH;
        if (battery > 30) return CLR_BAT_MED;
        return CLR_BAT_LOW;
    }

    // Distinct vehicle color palette
    private static final Color[] VEHICLE_COLORS = {
        new Color(220, 50, 50),    // Red
        new Color(50, 100, 220),   // Blue
        new Color(30, 160, 30),    // Green
        new Color(200, 130, 0),    // Orange
        new Color(150, 40, 190),   // Purple
        new Color(0, 170, 170),    // Cyan
        new Color(200, 50, 140),   // Pink
        new Color(130, 100, 30),   // Brown
        new Color(80, 80, 200),    // Indigo
        new Color(180, 180, 0),    // Yellow-green
        new Color(0, 130, 80),     // Teal
        new Color(180, 70, 70),    // Salmon
        new Color(90, 140, 200),   // Steel blue
        new Color(200, 90, 40),    // Rust
        new Color(120, 50, 120),   // Plum
        new Color(70, 70, 70),     // Dark gray
    };

    public static Color vehicleColor(int index) {
        return VEHICLE_COLORS[index % VEHICLE_COLORS.length];
    }
}