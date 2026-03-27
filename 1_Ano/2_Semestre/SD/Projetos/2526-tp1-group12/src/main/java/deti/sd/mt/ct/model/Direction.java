package deti.sd.mt.ct.model;

public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case WEST  -> EAST;
            case SOUTH -> NORTH;
            case EAST  -> WEST;
        };
    }

    public Direction rightOf() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST  -> SOUTH;
            case SOUTH -> WEST;
            case WEST  -> NORTH;
        };
    }

    public Direction leftOf() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST  -> SOUTH;
            case SOUTH -> EAST;
            case EAST  -> NORTH;
        };
    }
}

