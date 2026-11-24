package fr.sanctumcraft.instance;

/** Role / shape of a room in the sanctum graph. */
public enum RoomKind {
    /** Start room (1x1): spawn point + exit portal. Always on the north edge of the grid. */
    ENTRANCE,
    /** Nest candidate (1x1): may or may not hold an active egg nest. */
    NEST,
    /** Destination (1x1): mechanism that opens a second exit portal. Farthest room. */
    DESTINATION,
    /** Regular room (1x1). */
    HALL,
    /** Great hall (2x2 cells). */
    GREAT_HALL,
    /** Small room at a dead end (1x1 cell, smaller interior). */
    SMALL,
    /** Corridor (1x1 cell), built by code, only toward its doors. */
    CORRIDOR
}
